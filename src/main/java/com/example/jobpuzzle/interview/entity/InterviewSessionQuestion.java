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

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interview_session_question", uniqueConstraints = {
        @UniqueConstraint(name = "uk_session_question", columnNames = {"session_id", "question_id"}),
        @UniqueConstraint(name = "uk_session_question_order", columnNames = {"session_id", "display_order"})
})
public class InterviewSessionQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_question_id")
    private Long sessionQuestionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type_snapshot", nullable = false, length = 30)
    private InterviewQuestionType questionTypeSnapshot;

    @Lob
    @Column(name = "question_text_snapshot", nullable = false, columnDefinition = "TEXT")
    private String questionTextSnapshot;

    @Lob
    @Column(name = "intent_snapshot", columnDefinition = "TEXT")
    private String intentSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_focus_snapshot", columnDefinition = "json")
    private List<InterviewQuestionEvaluationFocus> evaluationFocusSnapshot;

    @Column(name = "related_match_id")
    private Long relatedMatchId;

    @Column(name = "related_requirement_id", length = 100)
    private String relatedRequirementId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_refs_snapshot", columnDefinition = "json")
    private List<AnalysisSourceReference> sourceRefsSnapshot;

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
        InterviewSessionQuestion snapshot = new InterviewSessionQuestion();
        snapshot.session = session;
        snapshot.question = question;
        snapshot.questionTypeSnapshot = question.getQuestionType();
        snapshot.questionTextSnapshot = question.getQuestion();
        snapshot.intentSnapshot = question.getIntent();
        snapshot.evaluationFocusSnapshot = question.getEvaluationFocus() == null
                ? List.of()
                : List.copyOf(question.getEvaluationFocus());
        snapshot.relatedMatchId = question.getRelatedMatch() == null
                ? null
                : question.getRelatedMatch().getMatchId();
        snapshot.relatedRequirementId = question.getRelatedRequirementId();
        snapshot.sourceRefsSnapshot = question.getSourceRefs() == null
                ? List.of()
                : List.copyOf(question.getSourceRefs());
        snapshot.displayOrder = displayOrder;
        snapshot.status = InterviewSessionQuestionStatus.PENDING;
        return snapshot;
    }

    public void start() {
        if (status == InterviewSessionQuestionStatus.PENDING) {
            status = InterviewSessionQuestionStatus.IN_PROGRESS;
        }
    }

    public void complete() {
        status = InterviewSessionQuestionStatus.COMPLETED;
    }

    public void skip() {
        if (status == InterviewSessionQuestionStatus.PENDING) {
            status = InterviewSessionQuestionStatus.SKIPPED;
        }
    }
}
