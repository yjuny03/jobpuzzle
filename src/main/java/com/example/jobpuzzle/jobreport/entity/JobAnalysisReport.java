package com.example.jobpuzzle.jobreport.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "job_analysis_report")
public class JobAnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    private Long jobCategoryId;

    private Integer dataCount;

    private String reliabilityLevel;

    private String keywordSummary;

    private Boolean isPublished;

    private Long reviewedBy;

    private LocalDateTime generatedAt;

    private LocalDateTime lastUpdatedAt;

}
