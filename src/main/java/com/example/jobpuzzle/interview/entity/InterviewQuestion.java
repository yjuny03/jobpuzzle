package com.example.jobpuzzle.interview.entity;

<<<<<<< HEAD
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.analysis.entity.ConfirmedAnalysisSnapshot;
import com.example.jobpuzzle.global.common.BaseEntity;
=======
import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.analysis.entity.AnalysisSourceReference;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResult;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
>>>>>>> develop
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
<<<<<<< HEAD
import org.hibernate.Session;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
=======
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

>>>>>>> develop
import java.util.List;

@Getter
@Entity
<<<<<<< HEAD
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interview_question")
public class InterviewQuestion {
=======
@NoArgsConstructor
@Table(name = "interview_question", uniqueConstraints = {
        @UniqueConstraint(name = "uk_interview_question_set_key", columnNames = {"question_set_id", "question_key"}),
        @UniqueConstraint(name = "uk_interview_question_set_order", columnNames = {"question_set_id", "display_order"})
})
public class InterviewQuestion extends BaseTimeEntity {
>>>>>>> develop

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
<<<<<<< HEAD
    @JoinColumn(name = "session_id")
    private InterviewSession interviewSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private ConfirmedAnalysisSnapshot confirmedAnalysisSnapshot;


    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
=======
    @JoinColumn(name = "question_set_id", nullable = false)
    private QuestionSet questionSet;

    // AI questionId는 DB PK가 아니라 QuestionSet 안에서 유일한 questionKey로 저장한다.
    @Column(name = "question_key", nullable = false, length = 100)
    private String questionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
>>>>>>> develop
    private InterviewQuestionType questionType;


    @Lob
<<<<<<< HEAD
    @Column(name = "question_text",nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Lob
    @Column(name = "intent", columnDefinition = "TEXT")
    private String intent;

    // Hibernate에게 자바의 List<String>을 DB JSON으로 변환해서 저장하고, DB JSON을 다시 List<String>으로 읽어 와라.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_focus", columnDefinition = "json")
    private List<String> evaluationFocus;

    @Column(name = "related_requirement", length = 300)
    private String relatedRequirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
=======
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(name = "intent", nullable = false, columnDefinition = "TEXT")
    private String intent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_focus", nullable = false, columnDefinition = "json")
    private List<InterviewQuestionEvaluationFocus> evaluationFocus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_match_id")
    private MatchAnalysisResult relatedMatch;

    @Column(name = "related_requirement_id", length = 100)
    private String relatedRequirementId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_refs", nullable = false, columnDefinition = "json")
    private List<AnalysisSourceReference> sourceRefs;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
>>>>>>> develop
    private InterviewQuestionReviewStatus reviewStatus;

    @Lob
    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

<<<<<<< HEAD
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_question_id")
    private InterviewQuestion originQuestion;

    @Column(name = "display_order",nullable = false)
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id")
    private AiCallLog aiCallLog;

    @Builder
    private InterviewQuestion(
            // 기본/약점 보완 모드는 세션 생성 후 질문을 만들므로,
            // 생성 시점에 해당 세션을 함께 연결할 수 있다.
            InterviewSession session,

            ConfirmedAnalysisSnapshot snapshot,
            InterviewQuestionType questionType,
            String questionText,
            String intent,
            List<String> evaluationFocus,
            String relatedRequirement,
            String reviewNote,
            Integer displayOrder,
            InterviewQuestion originQuestion,
            AiCallLog aiCallLog
            ){
        this.interviewSession = session;
        this.confirmedAnalysisSnapshot = snapshot;
        this.questionType = questionType;
        this.questionText = questionText;
        this.intent = intent;
        this.evaluationFocus = evaluationFocus;
        this.relatedRequirement = relatedRequirement;
        this.reviewNote = reviewNote;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
        this.originQuestion = originQuestion;
        this.aiCallLog = aiCallLog;

        this.reviewStatus = InterviewQuestionReviewStatus.PASS;
    }

    // 맞춤 모드 질문은 분석 확정 시 snapshot에 먼저 저장된다.
    // 사용자가 실제 면접 세션을 생성한 뒤 해당 세션에 질문을 연결한다.
    public void assignToSession(InterviewSession session) {
        this.interviewSession = session;
=======
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // 최종 QuestionSet에는 자체 검수를 통과한 PASS 질문만 저장한다.
    public static InterviewQuestion from(QuestionSet questionSet, MatchAnalysisResult relatedMatch,
                                         CustomizedAnalysisGenerationResult.Question value, int displayOrder) {
        if (value.getReviewStatus() != InterviewQuestionReviewStatus.PASS) {
            throw new IllegalArgumentException("Only PASS questions can be persisted in a QuestionSet.");
        }
        InterviewQuestion question = new InterviewQuestion();
        question.questionSet = questionSet;
        question.relatedMatch = relatedMatch;
        question.questionKey = value.getQuestionId();
        question.questionType = value.getQuestionType();
        question.question = value.getQuestion();
        question.intent = value.getIntent();
        question.evaluationFocus = value.getEvaluationFocus() == null ? List.of() : List.copyOf(value.getEvaluationFocus());
        question.relatedRequirementId = value.getRelatedRequirementId();
        question.sourceRefs = sourceRefs(value.getSourceRefs());
        question.reviewStatus = value.getReviewStatus();
        question.reviewNote = value.getReviewNote();
        question.displayOrder = displayOrder;
        return question;
>>>>>>> develop
    }

    private static List<AnalysisSourceReference> sourceRefs(List<SourceReference> values) {
        return values == null ? List.of() : values.stream().map(AnalysisSourceReference::from).toList();
    }
}
