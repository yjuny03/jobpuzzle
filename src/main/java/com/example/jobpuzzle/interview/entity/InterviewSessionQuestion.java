package com.example.jobpuzzle.interview.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

// 사용자가 세션에 선택한 질문의 당시 내용·순서·평가기준을 불변 복사
@Getter
@Entity
@NoArgsConstructor
@Table(name = "interview_session_question", uniqueConstraints = {
        @UniqueConstraint(name = "uk_session_question", columnNames = {"session_id", "question_id"}),
        @UniqueConstraint(name = "uk_session_display_order", columnNames = {"session_id", "display_order"})
})
public class InterviewSessionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionQuestionId;

    @Column(nullable = false)
    private Long sessionId;

    // 원본 생성 질문
    @Column(nullable = false)
    private Long questionId;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionTextSnapshot;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String intentSnapshot;

    // JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private String evaluationFocusSnapshot;

    private String relatedRequirementId;

    // JSON, 기본값 []
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private String sourceRefsSnapshot;

    @Column(nullable = false)
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewSessionQuestionStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private InterviewSessionQuestion(
            Long sessionId,
            Long questionId,
            String questionTextSnapshot,
            String intentSnapshot,
            String evaluationFocusSnapshot,
            String relatedRequirementId,
            String sourceRefsSnapshot,
            Integer displayOrder
    ) {
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.questionTextSnapshot = questionTextSnapshot;
        this.intentSnapshot = intentSnapshot;
        this.evaluationFocusSnapshot = evaluationFocusSnapshot;
        this.relatedRequirementId = relatedRequirementId;
        this.sourceRefsSnapshot = sourceRefsSnapshot == null ? "[]" : sourceRefsSnapshot;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;

        this.status = InterviewSessionQuestionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // 원 질문 답변 제출 시 전환. 이후 평가·꼬리질문 진행이 남아있는 동안 유지
    public void startProgress() {
        this.status = InterviewSessionQuestionStatus.IN_PROGRESS;
    }

    // 질문 흐름 종료(원 질문 답변 미제출로 세션 완료 시 SKIPPED, 그 외 평가 종결 시 COMPLETED)
    public void complete() {
        this.status = InterviewSessionQuestionStatus.COMPLETED;
    }

    public void skip() {
        this.status = InterviewSessionQuestionStatus.SKIPPED;
    }
}
