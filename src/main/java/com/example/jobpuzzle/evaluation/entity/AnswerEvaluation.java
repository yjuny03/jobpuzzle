package com.example.jobpuzzle.evaluation.entity;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.interview.entity.InterviewMessage;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

// 사용자 답변 메시지별 평가 결과. 원 답변과 꼬리답변을 각각 저장
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "answer_evaluation")
@Check(constraints = "score >= 0 AND score <= 100")
public class AnswerEvaluation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Long evaluationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_message_id", nullable = false, unique = true)
    private InterviewMessage answerMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_question_id", nullable = false)
    private InterviewSessionQuestion sessionQuestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_mode", nullable = false, length = 30)
    private InterviewSessionMode evaluationMode;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "pass_threshold", nullable = false)
    private int passThreshold;

    @Column(name = "score_label", length = 100)
    private String scoreLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_detail", columnDefinition = "json")
    private Map<String, DimensionEvaluation> evaluationDetail;

    @Column(name = "target_weakness_tag", length = 100)
    private String targetWeaknessTag;

    @Column(name = "target_dimension", length = 50)
    private String targetDimension;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weakness_tags", columnDefinition = "json")
    private List<String> weaknessTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weakness_diagnostics", columnDefinition = "json")
    private Map<String, List<String>> weaknessDiagnostics;

    @Lob
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "improvement_direction",
            nullable = false,
            columnDefinition = "json"
    )
    private List<String> improvementDirection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id", nullable = false)
    private AiCallLog aiCallLog;

    public static AnswerEvaluation create(
            InterviewMessage answerMessage,
            InterviewSessionQuestion sessionQuestion,
            InterviewSessionMode mode,
            int score,
            int passThreshold,
            String scoreLabel,
            Map<String, DimensionEvaluation> evaluationDetail,
            String targetWeaknessTag,
            String targetDimension,
            List<String> weaknessTags,
            Map<String, List<String>> weaknessDiagnostics,
            String summary,
            List<String> improvementDirection,
            AiCallLog aiCallLog
    ) {
        AnswerEvaluation evaluation = new AnswerEvaluation();

        evaluation.answerMessage = answerMessage;
        evaluation.sessionQuestion = sessionQuestion;
        evaluation.evaluationMode = mode;
        evaluation.score = score;
        evaluation.passThreshold = passThreshold;
        evaluation.scoreLabel = scoreLabel;
        evaluation.evaluationDetail = evaluationDetail == null
                ? Map.of()
                : Map.copyOf(evaluationDetail);
        evaluation.targetWeaknessTag = targetWeaknessTag;
        evaluation.targetDimension = targetDimension;
        evaluation.weaknessTags = weaknessTags == null
                ? List.of()
                : List.copyOf(weaknessTags);
        evaluation.weaknessDiagnostics = weaknessDiagnostics == null
                ? Map.of()
                : weaknessDiagnostics.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
        evaluation.summary = summary;
        evaluation.improvementDirection = improvementDirection == null
                ? List.of()
                : List.copyOf(improvementDirection);
        evaluation.aiCallLog = aiCallLog;

        return evaluation;
    }

    // 기존 면접·평가 코드에서 사용
    public boolean passed() {
        return score >= passThreshold;
    }

    // develop의 최종 리포트 코드와 이름을 맞추기 위한 호환 메서드
    public boolean isPassed() {
        return passed();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionEvaluation {

        private Integer score;
        private String comment;
    }
}
