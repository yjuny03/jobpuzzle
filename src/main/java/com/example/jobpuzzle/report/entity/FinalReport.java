package com.example.jobpuzzle.report.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "final_report")
public class FinalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    private Long sessionId;

    private Double overallScore;

    private String scoreLabel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String categoryScores;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String evidenceSummary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String weaknessTagSummary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String nextPracticeRecommendation;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String learningDirection;

    private Long aiCallLogId;

    private LocalDateTime createdAt;

}
