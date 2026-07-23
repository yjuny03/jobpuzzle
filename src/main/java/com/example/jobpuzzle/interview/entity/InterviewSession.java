package com.example.jobpuzzle.interview.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

// 사용자가 선택한 질문으로 생성하는 면접 세션과 모드별 불변 기준 저장
@Getter
@Entity
@NoArgsConstructor
@Table(name = "interview_session")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    @Column(nullable = false)
    private Long userId;

    // 질문 선택의 원본 묶음
    @Column(nullable = false)
    private Long questionSetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewSessionMode mode;

    // COMPANY_FIT에서 필수. 동일 스냅샷으로 여러 세션 생성 가능
    private Long snapshotId;

    private Long jobPostingAnalysisId;

    private Long candidateAnalysisId;

    // 세션 생성 당시 직무 기준
    @Column(nullable = false)
    private Long jobCategoryId;

    // 세션 질문 생성에 사용한 가이드 컨텍스트
    private Long guideContextId;

    private Long guideId;

    private String guideVersion;

    // AI 질문 생성 프롬프트 버전. fallback 템플릿 질문 세션은 NULL
    private String promptVersion;

    // WEAKNESS_REVIEW 모드 약점 태그
    private String targetWeaknessTag;

    // WEAKNESS_REVIEW 질문 생성에 사용한 기준 평가 ID 목록(JSON). 다른 모드는 빈 배열
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private String basisEvaluationIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewSessionStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime canceledAt;

    private LocalDateTime deletedAt;

    @Builder
    private InterviewSession(
            Long userId,
            Long questionSetId,
            InterviewSessionMode mode,
            Long snapshotId,
            Long jobPostingAnalysisId,
            Long candidateAnalysisId,
            Long jobCategoryId,
            Long guideContextId,
            Long guideId,
            String guideVersion,
            String promptVersion,
            String targetWeaknessTag,
            String basisEvaluationIds
    ) {
        this.userId = userId;
        this.questionSetId = questionSetId;
        this.mode = mode;
        this.snapshotId = snapshotId;
        this.jobPostingAnalysisId = jobPostingAnalysisId;
        this.candidateAnalysisId = candidateAnalysisId;
        this.jobCategoryId = jobCategoryId;
        this.guideContextId = guideContextId;
        this.guideId = guideId;
        this.guideVersion = guideVersion;
        this.promptVersion = promptVersion;
        this.targetWeaknessTag = targetWeaknessTag;
        this.basisEvaluationIds = basisEvaluationIds == null ? "[]" : basisEvaluationIds;

        this.status = InterviewSessionStatus.CREATED;
        this.createdAt = LocalDateTime.now();
    }

    // 최초 원 질문 답변 제출 시 전환
    public void start() {
        if (this.status == InterviewSessionStatus.CREATED) {
            this.status = InterviewSessionStatus.IN_PROGRESS;
            this.startedAt = LocalDateTime.now();
        }
    }

    public void complete() {
        this.status = InterviewSessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = InterviewSessionStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return this.status == InterviewSessionStatus.COMPLETED;
    }
}
