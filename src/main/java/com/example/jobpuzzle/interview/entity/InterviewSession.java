package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.analysis.entity.ConfirmedAnalysisSnapshot;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interview_session")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", unique = true)
    private ConfirmedAnalysisSnapshot snapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private InterviewSessionMode mode;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private JobGuideDocument guideDocument;

    @Column(name = "guide_version")
    private String guideVersion;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private InterviewSessionStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "target_weakness_tag")
    private String targetWeaknessTag;

    @Builder
    private InterviewSession(
            User user,
            ConfirmedAnalysisSnapshot snapshot,
            InterviewSessionMode mode,
            JobGuideDocument guideDocument,
            String guideVersion,
            String promptVersion,
            String targetWeaknessTag
    ) {
        this.user = user;
        this.snapshot = snapshot;
        this.mode = mode;
        this.guideDocument = guideDocument;
        this.guideVersion = guideVersion;
        this.promptVersion = promptVersion;
        this.targetWeaknessTag = targetWeaknessTag;

        this.status = InterviewSessionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = InterviewSessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
