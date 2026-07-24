package com.example.jobpuzzle.document.dto;

import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 자료 원본 메타 응답 - filePath는 노출하지 않음
@Getter
public class DocumentResponse {

    private final Long documentId;
    private final UserDocumentType documentType;
    private final UserDocumentSourceType sourceType;
    private final String displayName;
    private final boolean keepOriginal;
    private final List<DocumentFileResponse> files;
    private final LocalDateTime createdAt;

    private DocumentResponse(
            Long documentId, UserDocumentType documentType, UserDocumentSourceType sourceType,
            String displayName, boolean keepOriginal, List<DocumentFileResponse> files, LocalDateTime createdAt
    ) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.sourceType = sourceType;
        this.displayName = displayName;
        this.keepOriginal = keepOriginal;
        this.files = files;
        this.createdAt = createdAt;
    }

    public static DocumentResponse from(UserDocument document) {
        return new DocumentResponse(
                document.getDocumentId(),
                document.getDocumentType(),
                document.getSourceType(),
                document.getDisplayName(),
                document.isKeepOriginal(),
                document.getFiles().stream()
                        .map(file -> new DocumentFileResponse(file.getFileName(), file.getPageOrder()))
                        .toList(),
                document.getCreatedAt()
        );
    }

    public record DocumentFileResponse(String fileName, int pageOrder) {
    }
}