package com.example.jobpuzzle.document.dto;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentExtractionStatus;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.Getter;

import java.time.LocalDateTime;

// JSON-00(DocumentExtractionResult) 계약 기반 응답. majorVersion/minorVersion이 둘 다 null이면
// 아직 한 번도 확정되지 않은 검토 중 버전(자유 수정 구간)이라는 뜻
@Getter
public class ExtractionVersionResponse {

    private final Long extractionId;
    private final Long documentId;
    private final UserDocumentType documentType;
    private final UserDocumentSourceType sourceType;
    private final Integer majorVersion;
    private final Integer minorVersion;
    private final DocumentExtractionStatus extractionStatus;
    private final DocumentVersionStatus versionStatus;
    private final String content;
    private final Long baseExtractionId;
    private final Integer pageCount;
    private final boolean ocrApplied;
    private final LocalDateTime createdAt;
    private final LocalDateTime confirmedAt;
    private final String failureReason;

    private ExtractionVersionResponse(
            Long extractionId, Long documentId, UserDocumentType documentType, UserDocumentSourceType sourceType,
            Integer majorVersion, Integer minorVersion,
            DocumentExtractionStatus extractionStatus, DocumentVersionStatus versionStatus, String content,
            Long baseExtractionId, Integer pageCount, boolean ocrApplied,
            LocalDateTime createdAt, LocalDateTime confirmedAt, String failureReason
    ) {
        this.extractionId = extractionId;
        this.documentId = documentId;
        this.documentType = documentType;
        this.sourceType = sourceType;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.extractionStatus = extractionStatus;
        this.versionStatus = versionStatus;
        this.content = content;
        this.baseExtractionId = baseExtractionId;
        this.pageCount = pageCount;
        this.ocrApplied = ocrApplied;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.failureReason = failureReason;
    }

    public static ExtractionVersionResponse from(DocumentExtraction extraction) {
        return new ExtractionVersionResponse(
                extraction.getExtractionId(),
                extraction.getDocument().getDocumentId(),
                extraction.getDocument().getDocumentType(),
                extraction.getDocument().getSourceType(),
                extraction.getMajorVersion(),
                extraction.getMinorVersion(),
                extraction.getExtractionStatus(),
                extraction.getVersionStatus(),
                extraction.getContent(),
                extraction.getBaseExtraction() != null ? extraction.getBaseExtraction().getExtractionId() : null,
                extraction.getPageCount(),
                extraction.isOcrApplied(),
                extraction.getCreatedAt(),
                extraction.getConfirmedAt(),
                extraction.getFailureReason()
        );
    }
}