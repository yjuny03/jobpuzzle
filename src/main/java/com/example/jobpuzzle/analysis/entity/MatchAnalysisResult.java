package com.example.jobpuzzle.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "match_analysis_result")
public class MatchAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchId;

    private Long snapshotId;

    private String requirement;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String candidateEvidence;

    private String matchLevel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String missingPoint;

    private Long aiCallLogId;

}
