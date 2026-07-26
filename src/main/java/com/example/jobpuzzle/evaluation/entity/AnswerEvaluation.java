package com.example.jobpuzzle.evaluation.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

// 사용자 답변 메시지별 평가 결과. 원 답변과 꼬리답변을 각각 저장
@Getter
@Entity
@NoArgsConstructor
@Table(name = "answer_evaluation")
@Check(constraints = "score >= 0 AND score <= 100")
public class AnswerEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long evaluationId;

    // 평가 대상 ORIGINAL_ANSWER 또는 FOLLOW_UP_ANSWER 메시지 (UNIQUE)
    @Column(nullable = false, unique = true)
    private Long answerMessageId;

    @Column(nullable = false)
    private Long sessionQuestionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerEvaluationMode evaluationMode;

    // 0~100 기준 충족도 점수
    @Column(nullable = false)
    private Integer score;

    // 약점 태그·해결 여부 판정에 실제 적용한 기준 점수. 평가 시점 값을 그대로 보존
    @Column(nullable = false)
    private Integer passThreshold;

    // WEAKNESS_REVIEW 재평가는 null
    private String scoreLabel;

    // 8개 관점 또는 단일 targetDimension 점수·코멘트
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String evaluationDetail;

    // WEAKNESS_REVIEW 재평가 대상 태그
    private String targetWeaknessTag;

    // WEAKNESS_REVIEW 재평가 단일 관점
    private String targetDimension;

    // JSON. COMPANY_FIT의 답변형 약점 태그. BASIC=[]·WEAKNESS_REVIEW=null
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String weaknessTags;

    // 평가 요약 또는 단일 관점 코멘트
    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    // JSON. 답변 보완 방향. 기본값 []
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private String improvementDirection;

    // 성공한 호출 로그
    @Column(nullable = false)
    private Long aiCallLogId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private AnswerEvaluation(
            Long answerMessageId,
            Long sessionQuestionId,
            AnswerEvaluationMode evaluationMode,
            Integer score,
            Integer passThreshold,
            String scoreLabel,
            String evaluationDetail,
            String targetWeaknessTag,
            String targetDimension,
            String weaknessTags,
            String summary,
            String improvementDirection,
            Long aiCallLogId
    ) {
        this.answerMessageId = answerMessageId;
        this.sessionQuestionId = sessionQuestionId;
        this.evaluationMode = evaluationMode;
        this.score = score;
        this.passThreshold = passThreshold;
        this.scoreLabel = scoreLabel;
        this.evaluationDetail = evaluationDetail;
        this.targetWeaknessTag = targetWeaknessTag;
        this.targetDimension = targetDimension;
        this.weaknessTags = weaknessTags;
        this.summary = summary;
        this.improvementDirection = improvementDirection == null ? "[]" : improvementDirection;
        this.aiCallLogId = aiCallLogId;

        this.createdAt = LocalDateTime.now();
    }

    // score >= passThreshold이면 통과
    public boolean isPassed() {
        return score != null && passThreshold != null && score >= passThreshold;
    }
}
