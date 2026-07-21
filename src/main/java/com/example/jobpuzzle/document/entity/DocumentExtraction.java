package com.example.jobpuzzle.document.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "document_extraction")
public class DocumentExtraction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "extraction_id")
    private Long extractionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private UserDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_extraction_id")
    private DocumentExtraction baseExtraction;

    @Column(name = "version")
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 20)
    private DocumentExtractionStatus extractionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "version_status", length = 20)
    private DocumentVersionStatus versionStatus;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "ocr_applied", nullable = false)
    private boolean ocrApplied = false;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Builder
    private DocumentExtraction(
            UserDocument document,
            DocumentExtraction baseExtraction,
            Integer version,
            DocumentExtractionStatus extractionStatus,
            DocumentVersionStatus versionStatus,
            String content,
            Integer pageCount,
            boolean ocrApplied,
            String failureReason
    ) {
        this.document = document;
        this.baseExtraction = baseExtraction;
        this.version = version;
        this.extractionStatus = extractionStatus;
        this.versionStatus = versionStatus;
        this.content = content;
        this.pageCount = pageCount;
        this.ocrApplied = ocrApplied;
        this.failureReason = failureReason;
    }

    public void confirm() {
        this.versionStatus = DocumentVersionStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void supersede() {
        this.versionStatus = DocumentVersionStatus.SUPERSEDED;
    }
}