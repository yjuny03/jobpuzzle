package com.example.jobpuzzle.evaluation.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.interview.entity.InterviewSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "weakness_remediation_attempt", uniqueConstraints = {
        @UniqueConstraint(name = "uk_weakness_attempt_evaluation", columnNames = "evaluation_id")
})
public class WeaknessRemediationAttempt extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long attemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_tag_log_id", nullable = false)
    private WeaknessTagLog originTagLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private AnswerEvaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WeaknessRemediationStatus status;

    public static WeaknessRemediationAttempt create(
            WeaknessTagLog originTagLog,
            AnswerEvaluation evaluation
    ) {
        WeaknessRemediationAttempt attempt = new WeaknessRemediationAttempt();
        attempt.originTagLog = originTagLog;
        attempt.evaluation = evaluation;
        attempt.session = evaluation.getSessionQuestion().getSession();
        attempt.status = evaluation.passed()
                ? WeaknessRemediationStatus.RESOLVED
                : WeaknessRemediationStatus.UNRESOLVED;
        return attempt;
    }
}
