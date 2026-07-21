package com.example.jobpuzzle.document.service;

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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentExtractionService {

    private final DocumentExtractionRepository documentExtractionRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final DocumentExtractionAsyncRunner asyncRunner;

    // 202 Accepted로 즉시 응답하고, 실제 추출은 DocumentExtractionAsyncRunner가 비동기로 수행
    public ExtractionJobResponse extractDocumentText(Long userId, Long documentId) {
        UserDocument document = findOwnedDocument(userId, documentId);
        asyncRunner.run(document.getDocumentId());
        return ExtractionJobResponse.accepted(documentId);
    }

    // 저장된 버전은 절대 수정하지 않고, base 버전을 기준으로 새 DRAFT 버전 생성
    @Transactional
    public ExtractionVersionResponse saveEditedVersion(Long userId, Long baseExtractionId, ExtractionEditRequest request) {
        DocumentExtraction base = findOwnedExtraction(userId, baseExtractionId);
        Long documentId = base.getDocument().getDocumentId();
        int nextVersion = documentExtractionRepository.findMaxVersionByDocumentId(documentId)
                .map(v -> v + 1)
                .orElse(1);

        DocumentExtraction newVersion = DocumentExtraction.builder()
                .document(base.getDocument())
                .baseExtraction(base)
                .version(nextVersion)
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
    @Transactional
    public ExtractionVersionResponse confirmExtraction(Long userId, Long extractionId) {
        DocumentExtraction target = findOwnedExtraction(userId, extractionId);
        Long documentId = target.getDocument().getDocumentId();

        DocumentExtraction latestDraft = documentExtractionRepository
                .findTopByDocument_DocumentIdAndVersionStatusOrderByVersionDesc(documentId, DocumentVersionStatus.DRAFT)
                .orElseThrow(() -> new CustomException(ErrorCode.EXTRACTION_NOT_CONFIRMABLE));

        if (!latestDraft.getExtractionId().equals(extractionId)) {
            throw new CustomException(ErrorCode.EXTRACTION_NOT_LATEST_DRAFT);
        }

        documentExtractionRepository
                .findTopByDocument_DocumentIdAndVersionStatusOrderByVersionDesc(documentId, DocumentVersionStatus.CONFIRMED)
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
        return documentExtractionRepository.findByDocument_DocumentIdOrderByVersionDesc(documentId).stream()
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