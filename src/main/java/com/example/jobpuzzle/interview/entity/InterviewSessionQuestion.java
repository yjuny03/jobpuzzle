package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.analysis.entity.AnalysisSourceReference;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

// 사용자가 세션에 선택한 질문의 당시 내용·순서·평가기준을 불변 복사
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "interview_session_question",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_question",
                        columnNames = {"session_id", "question_id"}
                ),
                @UniqueConstraint(
                        name = "uk_session_display_order",
                        columnNames = {"session_id", "display_order"}
                )
        }
)
public class InterviewSessionQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_question_id")
    private Long sessionQuestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    // 원본 생성 질문
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    // 세션 생성 당시 질문 본문
    @Lob
    @Column(
            name = "question_text_snapshot",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String questionTextSnapshot;

    // 세션 생성 당시 출제 의도
    @Lob
    @Column(
            name = "intent_snapshot",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String intentSnapshot;

    // 세션 생성 당시 평가 관점
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "evaluation_focus_snapshot",
            nullable = false,
            columnDefinition = "json"
    )
    private List<InterviewQuestionEvaluationFocus> evaluationFocusSnapshot;

    // 세션 생성 당시 연결 요구사항
    @Column(name = "related_requirement_id", length = 100)
    private String relatedRequirementId;

    // 세션 생성 당시 질문 근거
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "source_refs_snapshot",
            nullable = false,
            columnDefinition = "json"
    )
    private List<AnalysisSourceReference> sourceRefsSnapshot;

    // 세션 진행 순서
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InterviewSessionQuestionStatus status;

    public static InterviewSessionQuestion snapshot(
            InterviewSession session,
            InterviewQuestion question,
            int displayOrder
    ) {
        InterviewSessionQuestion snapshot =
                new InterviewSessionQuestion();

        snapshot.session = session;
        snapshot.question = question;
        snapshot.questionTextSnapshot = question.getQuestion();
        snapshot.intentSnapshot = question.getIntent();

        snapshot.evaluationFocusSnapshot =
                question.getEvaluationFocus() == null
                        ? List.of()
                        : List.copyOf(question.getEvaluationFocus());

        snapshot.relatedRequirementId =
                question.getRelatedRequirementId();

        snapshot.sourceRefsSnapshot =
                question.getSourceRefs() == null
                        ? List.of()
                        : List.copyOf(question.getSourceRefs());

        snapshot.displayOrder = displayOrder;
        snapshot.status = InterviewSessionQuestionStatus.PENDING;

        return snapshot;
    }

    // 원 질문 답변 제출 시 IN_PROGRESS로 전환
    public void start() {
        if (status == InterviewSessionQuestionStatus.PENDING) {
            status = InterviewSessionQuestionStatus.IN_PROGRESS;
        }
    }

    // 원 질문과 필요한 꼬리질문 흐름이 모두 끝났을 때 호출
    public void complete() {
        status = InterviewSessionQuestionStatus.COMPLETED;
    }

    // 세션 완료 시 원 질문 답변을 제출하지 않은 질문만 SKIPPED 처리
    public void skip() {
        if (status == InterviewSessionQuestionStatus.PENDING) {
            status = InterviewSessionQuestionStatus.SKIPPED;
        }
    }
}