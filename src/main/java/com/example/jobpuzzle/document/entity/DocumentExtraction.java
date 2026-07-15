package com.example.jobpuzzle.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "document_extraction")
public class DocumentExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long extractionId;

    private Long documentId;

    private DocumentExtractionStatus extractionStatus;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String editedText;

    private Integer pageCount;

    private Boolean ocrApplied;

    private Boolean editable;

    private String failureReason;

    private LocalDateTime createdAt;

}
