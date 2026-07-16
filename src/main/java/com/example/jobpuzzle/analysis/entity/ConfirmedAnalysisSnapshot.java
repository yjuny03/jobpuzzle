package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "confirmed_analysis_snapshot")
public class ConfirmedAnalysisSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_analysis_id", nullable = false)
    private JobPostingAnalysis jobPostingAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_analysis_id", nullable = false)
    private CandidateMaterialAnalysis candidateAnalysis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by", nullable = false)
    private User confirmedBy;

    @CreationTimestamp
    @Column(name = "confirmed_at", nullable = false, updatable = false)
    private LocalDateTime confirmedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConfirmedAnalysisSnapshotStatus status;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Builder
    private ConfirmedAnalysisSnapshot(
            JobPostingAnalysis jobPostingAnalysis,
            CandidateMaterialAnalysis candidateAnalysis,
            User confirmedBy
    ) {
        this.jobPostingAnalysis = jobPostingAnalysis;
        this.candidateAnalysis = candidateAnalysis;
        this.confirmedBy = confirmedBy;
        this.status = ConfirmedAnalysisSnapshotStatus.ACTIVE;
    }
}