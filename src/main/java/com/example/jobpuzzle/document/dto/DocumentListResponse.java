package com.example.jobpuzzle.document.dto;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DocumentListResponse {

    private final Long documentId;
    private final UserDocumentType documentType;
    private final String displayName;
    private final UserDocumentSourceType sourceType;
    private final Integer latestMajorVersion;
    private final Integer latestMinorVersion;
    private final DocumentVersionStatus latestVersionStatus;
    private final LocalDateTime deletedAt;
    private final LocalDateTime updatedAt;

    private DocumentListResponse(
            Long documentId, UserDocumentType documentType, String displayName, UserDocumentSourceType sourceType,
            Integer latestMajorVersion, Integer latestMinorVersion, DocumentVersionStatus latestVersionStatus,
            LocalDateTime deletedAt, LocalDateTime updatedAt
    ) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.displayName = displayName;
        this.sourceType = sourceType;
        this.latestMajorVersion = latestMajorVersion;
        this.latestMinorVersion = latestMinorVersion;
        this.latestVersionStatus = latestVersionStatus;
        this.deletedAt = deletedAt;
        this.updatedAt = updatedAt;
    }

    // latestExtraction은 해당 자료의 최신 저장 버전(있다면 그 버전, 없으면 null)
    public static DocumentListResponse of(UserDocument document, DocumentExtraction latestExtraction) {
        return new DocumentListResponse(
                document.getDocumentId(),
                document.getDocumentType(),
                document.getDisplayName(),
                document.getSourceType(),
                latestExtraction != null ? latestExtraction.getMajorVersion() : null,
                latestExtraction != null ? latestExtraction.getMinorVersion() : null,
                latestExtraction != null ? latestExtraction.getVersionStatus() : null,
                document.getDeletedAt(),
                document.getUpdatedAt()
        );
    }
}