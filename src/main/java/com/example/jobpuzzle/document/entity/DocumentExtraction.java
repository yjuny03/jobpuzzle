package com.example.jobpuzzle.document.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 20)
    private DocumentExtractionStatus extractionStatus;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Lob
    @Column(name = "edited_text", columnDefinition = "LONGTEXT")
    private String editedText;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "ocr_applied", nullable = false)
    private boolean ocrApplied = false;

    @Column(name = "editable", nullable = false)
    private boolean editable = true;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Builder
    private DocumentExtraction(
            UserDocument document,
            DocumentExtractionStatus extractionStatus,
            String extractedText,
            String editedText,
            Integer pageCount,
            boolean ocrApplied,
            boolean editable,
            String failureReason
    ) {
        this.document = document;
        this.extractionStatus = extractionStatus;
        this.extractedText = extractedText;
        this.editedText = editedText;
        this.pageCount = pageCount;
        this.ocrApplied = ocrApplied;
        this.editable = editable;
        this.failureReason = failureReason;
    }

    public void updateEditedText(String editedText) {
        if (!editable) {
            throw new CustomException(
                    ErrorCode.DOCUMENT_EXTRACTION_NOT_EDITABLE
            );
        }

        this.editedText = editedText;
    }
}