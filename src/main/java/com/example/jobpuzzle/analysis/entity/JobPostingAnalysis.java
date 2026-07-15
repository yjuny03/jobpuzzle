package com.example.jobpuzzle.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "job_posting_analysis")
public class JobPostingAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisId;

    private Long jobPostingId;

    private Long jobCategoryId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String mainTasks;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String preferred;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String companyValues;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String coreCompetencies;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String missingEvidence;

    private Boolean isEdited;

    private Long aiCallLogId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
