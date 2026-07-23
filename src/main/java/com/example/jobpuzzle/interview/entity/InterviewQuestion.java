package com.example.jobpuzzle.interview.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

// QuestionSet에 저장된 세션 생성 전 원 질문. 질문 자체검수와 근거 추적 포함
@Getter
@Entity
@NoArgsConstructor
@Table(name = "interview_question", uniqueConstraints = {
        @UniqueConstraint(name = "uk_question_set_key", columnNames = {"question_set_id", "question_key"})
})
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(nullable = false)
    private Long questionSetId;

    // AI 응답의 questionId
    @Column(nullable = false)
    private String questionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewQuestionType questionType;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String intent;

    // JSON. WEAKNESS_REVIEW는 단일 관점
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private String evaluationFocus;

    // 맞춤 질문의 연결 분석 항목
    private Long relatedMatchId;

    // 맞춤 질문의 공고 요구사항 식별자
    private String relatedRequirementId;

    // JSON, 기본값 []. 기본·약점 질문은 빈 배열 가능
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private String sourceRefs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewQuestionReviewStatus reviewStatus;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    // WEAKNESS_REVIEW 질문 생성의 기준 평가 ID. 질문별 1건
    private Long originEvaluationId;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private InterviewQuestion(
            Long questionSetId,
            String questionKey,
            InterviewQuestionType questionType,
            String questionText,
            String intent,
            String evaluationFocus,
            Long relatedMatchId,
            String relatedRequirementId,
            String sourceRefs,
            InterviewQuestionReviewStatus reviewStatus,
            String reviewNote,
            Long originEvaluationId,
            Integer displayOrder
    ) {
        this.questionSetId = questionSetId;
        this.questionKey = questionKey;
        this.questionType = questionType;
        this.questionText = questionText;
        this.intent = intent;
        this.evaluationFocus = evaluationFocus;
        this.relatedMatchId = relatedMatchId;
        this.relatedRequirementId = relatedRequirementId;
        this.sourceRefs = sourceRefs == null ? "[]" : sourceRefs;
        this.reviewStatus = reviewStatus == null ? InterviewQuestionReviewStatus.PASS : reviewStatus;
        this.reviewNote = reviewNote;
        this.originEvaluationId = originEvaluationId;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;

        this.createdAt = LocalDateTime.now();
    }
}
