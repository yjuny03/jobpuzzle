package com.example.jobpuzzle.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "candidate_material_analysis")
public class CandidateMaterialAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisId;

    private Long userId;

    private Long jobCategoryId;

    private Long resumeDocumentId;

    private Long coverLetterDocumentId;

    private Long portfolioDocumentId;

    private Long experienceNoteDocumentId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String resumeAnalysis;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String coverLetterAnalysis;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String portfolioAnalysis;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String experienceNoteAnalysis;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String missingEvidence;

    private Boolean isEdited;

    private Long aiCallLogId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
