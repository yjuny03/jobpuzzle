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
    private final Integer latestVersion;
    private final DocumentVersionStatus latestVersionStatus;
    private final LocalDateTime deletedAt;
    private final LocalDateTime updatedAt;

    private DocumentListResponse(
            Long documentId, UserDocumentType documentType, String displayName, UserDocumentSourceType sourceType,
            Integer latestVersion, DocumentVersionStatus latestVersionStatus,
            LocalDateTime deletedAt, LocalDateTime updatedAt
    ) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.displayName = displayName;
        this.sourceType = sourceType;
        this.latestVersion = latestVersion;
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
                latestExtraction != null ? latestExtraction.getVersion() : null,
                latestExtraction != null ? latestExtraction.getVersionStatus() : null,
                document.getDeletedAt(),
                document.getUpdatedAt()
        );
    }
}