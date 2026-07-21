package com.example.jobpuzzle.document.service;

import com.example.jobpuzzle.document.dto.ChangeType;
import com.example.jobpuzzle.document.dto.ExtractionEditRequest;
import com.example.jobpuzzle.document.dto.ExtractionJobResponse;
import com.example.jobpuzzle.document.dto.ExtractionVersionResponse;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentExtractionStatus;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.extraction.DocumentExtractionAsyncRunner;
import com.example.jobpuzzle.document.repository.DocumentExtractionRepository;
import com.example.jobpuzzle.document.repository.UserDocumentRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentExtractionService {

    private final DocumentExtractionRepository documentExtractionRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final DocumentExtractionAsyncRunner asyncRunner;

    // 202 Accepted로 즉시 응답하고, 실제 추출은 DocumentExtractionAsyncRunner가 비동기로 수행.
    // 이미 한 번이라도 확정된 적 있는 자료는 재추출 대신 수정 저장으로 유도
    public ExtractionJobResponse extractDocumentText(Long userId, Long documentId) {
        UserDocument document = findOwnedDocument(userId, documentId);

        boolean everVersioned = documentExtractionRepository
                .findTopByDocument_DocumentIdAndMajorVersionIsNotNullOrderByExtractionIdDesc(documentId)
                .isPresent();
        if (everVersioned) {
            throw new CustomException(ErrorCode.EXTRACTION_ALREADY_VERSIONED);
        }

        asyncRunner.run(document.getDocumentId());
        return ExtractionJobResponse.accepted(documentId);
    }

    // 저장된 버전은 절대 수정하지 않고, base 버전을 기준으로 새 DRAFT 버전 생성
    // 이 문서가 아직 한 번도 확정된 적 없으면(검토 중 구간) 계속 버전 번호 없이 저장하고,
    // 이미 확정 이력이 있으면 changeType(자잘한/큰 수정)에 따라 바로 다음 버전 번호를 부여
    @Transactional
    public ExtractionVersionResponse saveEditedVersion(Long userId, Long baseExtractionId, ExtractionEditRequest request) {
        DocumentExtraction base = findOwnedExtraction(userId, baseExtractionId);
        Long documentId = base.getDocument().getDocumentId();

        Optional<DocumentExtraction> latestVersioned = documentExtractionRepository
                .findTopByDocument_DocumentIdAndMajorVersionIsNotNullOrderByExtractionIdDesc(documentId);

        Integer newMajorVersion = null;
        Integer newMinorVersion = null;
        if (latestVersioned.isPresent()) {
            if (request.getChangeType() == null) {
                throw new CustomException(ErrorCode.CHANGE_TYPE_REQUIRED);
            }
            DocumentExtraction latest = latestVersioned.get();
            if (request.getChangeType() == ChangeType.MAJOR) {
                newMajorVersion = latest.getMajorVersion() + 1;
                newMinorVersion = 0;
            } else {
                newMajorVersion = latest.getMajorVersion();
                newMinorVersion = latest.getMinorVersion() + 1;
            }
        }

        DocumentExtraction newVersion = DocumentExtraction.builder()
                .document(base.getDocument())
                .baseExtraction(base)
                .majorVersion(newMajorVersion)
                .minorVersion(newMinorVersion)
                .extractionStatus(DocumentExtractionStatus.SUCCESS)
                .versionStatus(DocumentVersionStatus.DRAFT)
                .content(request.getContent())
                .pageCount(base.getPageCount())
                .ocrApplied(base.isOcrApplied())
                .build();

        documentExtractionRepository.save(newVersion);
        return ExtractionVersionResponse.from(newVersion);
    }

    // 같은 자료의 최신 DRAFT만 확정할 수 있고, 기존 CONFIRMED가 있으면 SUPERSEDED로 전환
    // 아직 버전이 없는 DRAFT를 확정하는 거면(이 문서의 첫 확정) 엔티티 안에서 1.0이 자동으로 부여
    @Transactional
    public ExtractionVersionResponse confirmExtraction(Long userId, Long extractionId) {
        DocumentExtraction target = findOwnedExtraction(userId, extractionId);
        Long documentId = target.getDocument().getDocumentId();

        DocumentExtraction latestDraft = documentExtractionRepository
                .findTopByDocument_DocumentIdAndVersionStatusOrderByExtractionIdDesc(documentId, DocumentVersionStatus.DRAFT)
                .orElseThrow(() -> new CustomException(ErrorCode.EXTRACTION_NOT_CONFIRMABLE));

        if (!latestDraft.getExtractionId().equals(extractionId)) {
            throw new CustomException(ErrorCode.EXTRACTION_NOT_LATEST_DRAFT);
        }

        documentExtractionRepository
                .findTopByDocument_DocumentIdAndVersionStatusOrderByExtractionIdDesc(documentId, DocumentVersionStatus.CONFIRMED)
                .ifPresent(previous -> {
                    previous.supersede();
                    documentExtractionRepository.save(previous);
                });

        target.confirm();
        documentExtractionRepository.save(target);
        return ExtractionVersionResponse.from(target);
    }

    public List<ExtractionVersionResponse> getDocumentVersions(Long userId, Long documentId) {
        findOwnedDocument(userId, documentId);
        return documentExtractionRepository.findByDocument_DocumentIdOrderByExtractionIdDesc(documentId).stream()
                .map(ExtractionVersionResponse::from)
                .toList();
    }

    public ExtractionVersionResponse getExtractionVersion(Long userId, Long extractionId) {
        return ExtractionVersionResponse.from(findOwnedExtraction(userId, extractionId));
    }

    // 분석 모듈이 스냅샷을 만들 때 넘겨받은 extractionId들이 전부 본인 소유의 CONFIRMED 버전인지 검증
    public List<DocumentExtraction> getConfirmedExtractions(Long userId, List<Long> extractionIds) {
        List<DocumentExtraction> extractions =
                documentExtractionRepository.findByExtractionIdInAndDocument_User_UserId(extractionIds, userId);

        boolean allConfirmed = extractions.size() == extractionIds.size()
                && extractions.stream().allMatch(e -> e.getVersionStatus() == DocumentVersionStatus.CONFIRMED);
        if (!allConfirmed) {
            throw new CustomException(ErrorCode.EXTRACTION_NOT_CONFIRMED);
        }
        return extractions;
    }

    private UserDocument findOwnedDocument(Long userId, Long documentId) {
        return userDocumentRepository.findByDocumentIdAndUser_UserId(documentId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    private DocumentExtraction findOwnedExtraction(Long userId, Long extractionId) {
        return documentExtractionRepository.findByExtractionIdAndDocument_User_UserId(extractionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXTRACTION_NOT_FOUND));
    }
}