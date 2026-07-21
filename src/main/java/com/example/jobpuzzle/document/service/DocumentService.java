package com.example.jobpuzzle.document.service;

import com.example.jobpuzzle.document.dto.DirectDocumentRegisterRequest;
import com.example.jobpuzzle.document.dto.DocumentDetailResponse;
import com.example.jobpuzzle.document.dto.DocumentListResponse;
import com.example.jobpuzzle.document.dto.DocumentResponse;
import com.example.jobpuzzle.document.dto.DocumentUploadRequest;
import com.example.jobpuzzle.document.dto.ExtractionVersionResponse;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentExtractionStatus;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.document.repository.DocumentExtractionRepository;
import com.example.jobpuzzle.document.repository.UserDocumentRepository;
import com.example.jobpuzzle.document.storage.FileStorage;
import com.example.jobpuzzle.global.common.dto.PageResponse;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final Set<UserDocumentType> DIRECT_INPUT_ALLOWED_TYPES = Set.of(
            UserDocumentType.JOB_POSTING, UserDocumentType.COMPANY_INFO, UserDocumentType.EXPERIENCE_NOTE
    );

    private final UserDocumentRepository userDocumentRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    @Transactional
    public DocumentResponse uploadDocument(Long userId, DocumentUploadRequest request) {
        UserDocumentSourceType sourceType = resolveSourceType(request.getFile());
        String storagePath = fileStorage.store(request.getFile(), request.getDocumentType());

        UserDocument document = UserDocument.builder()
                .user(userRepository.getReferenceById(userId))
                .documentType(request.getDocumentType())
                .sourceType(sourceType)
                .displayName(request.getDisplayName())
                .filePath(storagePath)
                .fileName(request.getFile().getOriginalFilename())
                .keepOriginal(request.isKeepOriginal())
                .build();

        userDocumentRepository.save(document);
        return DocumentResponse.from(document);
    }

    // 직접 입력은 추출 파이프라인을 거치지 않고 그 자리에서 미확정 DRAFT까지 생성 (버전은 첫 확정 시점에 부여)
    @Transactional
    public ExtractionVersionResponse registerTextDocument(Long userId, DirectDocumentRegisterRequest request) {
        if (!DIRECT_INPUT_ALLOWED_TYPES.contains(request.getDocumentType())) {
            throw new CustomException(ErrorCode.INVALID_DIRECT_INPUT_TYPE);
        }

        UserDocument document = UserDocument.builder()
                .user(userRepository.getReferenceById(userId))
                .documentType(request.getDocumentType())
                .sourceType(UserDocumentSourceType.TEXT)
                .displayName(request.getDisplayName())
                .filePath(null)
                .fileName(null)
                .keepOriginal(false)
                .build();
        userDocumentRepository.save(document);

        DocumentExtraction extraction = DocumentExtraction.builder()
                .document(document)
                .extractionStatus(DocumentExtractionStatus.SUCCESS)
                .versionStatus(DocumentVersionStatus.DRAFT)
                .content(request.getContent())
                .ocrApplied(false)
                .build();
        documentExtractionRepository.save(extraction);

        return ExtractionVersionResponse.from(extraction);
    }

    public PageResponse<DocumentListResponse> getDocumentList(Long userId, UserDocumentType documentType, Pageable pageable) {
        Page<UserDocument> documents = documentType != null
                ? userDocumentRepository.findByUser_UserIdAndDocumentTypeAndDeletedAtIsNull(userId, documentType, pageable)
                : userDocumentRepository.findByUser_UserIdAndDeletedAtIsNull(userId, pageable);

        Page<DocumentListResponse> response = documents.map(document -> {
            DocumentExtraction latest = documentExtractionRepository
                    .findByDocument_DocumentIdOrderByExtractionIdDesc(document.getDocumentId())
                    .stream()
                    .findFirst()
                    .orElse(null);
            return DocumentListResponse.of(document, latest);
        });

        return PageResponse.from(response);
    }

    public DocumentDetailResponse getDocumentDetail(Long userId, Long documentId) {
        UserDocument document = findOwnedDocument(userId, documentId);
        List<DocumentExtraction> versions =
                documentExtractionRepository.findByDocument_DocumentIdOrderByExtractionIdDesc(documentId);

        ExtractionVersionResponse latest = versions.stream()
                .findFirst()
                .map(ExtractionVersionResponse::from)
                .orElse(null);
        List<ExtractionVersionResponse> confirmedVersions = versions.stream()
                .filter(v -> v.getVersionStatus() == DocumentVersionStatus.CONFIRMED)
                .map(ExtractionVersionResponse::from)
                .toList();

        return DocumentDetailResponse.of(DocumentResponse.from(document), latest, confirmedVersions);
    }

    // 원본 파일과 추출·버전 이력은 물리 삭제하지 않고, 신규 조회·분석 대상에서만 제외
    @Transactional
    public void deleteDocument(Long userId, Long documentId) {
        UserDocument document = findOwnedDocument(userId, documentId);
        document.softDelete();
    }

    @Transactional
    public void updateOriginalFileRetention(Long userId, Long documentId, boolean keepOriginal) {
        UserDocument document = findOwnedDocument(userId, documentId);
        if (!keepOriginal && document.getFilePath() != null) {
            fileStorage.delete(document.getFilePath());
            document.clearFilePath();
        }
        document.updateKeepOriginal(keepOriginal);
    }

    private UserDocument findOwnedDocument(Long userId, Long documentId) {
        return userDocumentRepository.findByDocumentIdAndUser_UserId(documentId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    private UserDocumentSourceType resolveSourceType(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        return "pdf".equals(extension) ? UserDocumentSourceType.PDF : UserDocumentSourceType.IMAGE;
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        return originalFileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}