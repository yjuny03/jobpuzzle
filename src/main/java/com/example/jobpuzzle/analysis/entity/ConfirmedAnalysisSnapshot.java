package com.example.jobpuzzle.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "confirmed_analysis_snapshot")
public class ConfirmedAnalysisSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long snapshotId;

    private Long jobPostingAnalysisId;

    private Long candidateAnalysisId;

    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    private String status;

    private LocalDateTime canceledAt;

    private LocalDateTime usedAt;

}
