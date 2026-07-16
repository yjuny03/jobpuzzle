package com.example.jobpuzzle.document.entity;

import com.example.jobpuzzle.global.common.BaseEntity;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "user_document")
public class UserDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private UserDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private UserDocumentSourceType sourceType;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "keep_original", nullable = false)
    private boolean keepOriginal = false;

    @Column(name = "version", nullable = false)
    private int version = 1;

    @Column(name = "is_latest", nullable = false)
    private boolean isLatest = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private UserDocument(
            User user,
            UserDocumentType documentType,
            UserDocumentSourceType sourceType,
            String filePath,
            String fileName,
            boolean keepOriginal,
            int version
    ) {
        this.user = user;
        this.documentType = documentType;
        this.sourceType = sourceType;
        this.filePath = filePath;
        this.fileName = fileName;
        this.keepOriginal = keepOriginal;
        this.version = version <= 0 ? 1 : version;
        this.isLatest = true;
    }

    public void markAsPreviousVersion() {
        this.isLatest = false;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.isLatest = false;
    }
}