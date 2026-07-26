package com.example.jobpuzzle.report.entity;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

// 모든 COMPLETED 면접 모드의 세션 종합 리포트
@Getter
@Entity
@NoArgsConstructor
@Table(name = "final_report")
@Check(constraints = "overall_score >= 0 AND overall_score <= 100")
public class FinalReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    // 대상 COMPLETED 세션. 1:1
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private InterviewSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_mode", nullable = false, length = 20)
    private InterviewSessionMode interviewMode;

    @Column(name = "total_question_count", nullable = false)
    private int totalQuestionCount;

    @Column(name = "submitted_question_count", nullable = false)
    private int submittedQuestionCount;

    @Column(name = "evaluated_question_count", nullable = false)
    private int evaluatedQuestionCount;

    @Column(name = "evaluation_failed_question_count", nullable = false)
    private int evaluationFailedQuestionCount;

    @Column(name = "skipped_question_count", nullable = false)
    private int skippedQuestionCount;

    // submitted_question_count / total_question_count * 100, 생성 시점 값 고정 저장
    @Column(name = "completion_rate", nullable = false)
    private int completionRate;

    // 실제로 평가한 관점만 반영한 종합 기준 충족도
    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "score_label", nullable = false)
    private String scoreLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_scores", columnDefinition = "json", nullable = false)
    private CategoryScores categoryScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "basis_summary", columnDefinition = "json", nullable = false)
    private BasisSummary basisSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weakness_tag_summary", columnDefinition = "json", nullable = false)
    private List<WeaknessTagSummary> weaknessTagSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_practice_recommendation", columnDefinition = "json", nullable = false)
    private List<NextPracticeRecommendation> nextPracticeRecommendation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "learning_direction", columnDefinition = "json", nullable = false)
    private List<String> learningDirection;

    // 성공한 호출 로그
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id", nullable = false)
    private AiCallLog aiCallLog;

    @Builder
    public FinalReport(
            InterviewSession session,
            InterviewSessionMode interviewMode,
            int totalQuestionCount,
            int submittedQuestionCount,
            int evaluatedQuestionCount,
            int evaluationFailedQuestionCount,
            int skippedQuestionCount,
            int completionRate,
            int overallScore,
            String scoreLabel,
            CategoryScores categoryScores,
            BasisSummary basisSummary,
            List<WeaknessTagSummary> weaknessTagSummary,
            List<NextPracticeRecommendation> nextPracticeRecommendation,
            List<String> learningDirection,
            AiCallLog aiCallLog
    ) {
        this.session = session;
        this.interviewMode = interviewMode;
        this.totalQuestionCount = totalQuestionCount;
        this.submittedQuestionCount = submittedQuestionCount;
        this.evaluatedQuestionCount = evaluatedQuestionCount;
        this.evaluationFailedQuestionCount = evaluationFailedQuestionCount;
        this.skippedQuestionCount = skippedQuestionCount;
        this.completionRate = completionRate;
        this.overallScore = overallScore;
        this.scoreLabel = scoreLabel;
        this.categoryScores = categoryScores;
        this.basisSummary = basisSummary;
        this.weaknessTagSummary = weaknessTagSummary;
        this.nextPracticeRecommendation = nextPracticeRecommendation;
        this.learningDirection = learningDirection;
        this.aiCallLog = aiCallLog;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryScores {
        private Integer intentMatch;
        private Integer specificity;
        private Integer ownRole;
        private Integer problemSolving;
        private Integer resultExpression;
        private Integer requirementConnection;
        private Integer guideAlignment;
        private Integer deliveryClarity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BasisSummary {
        private String jobCategory;
        private JobCategoryCareerLevel careerLevel;
        private Integer evaluationPassThreshold;
        private UsedGuide usedGuide;
        private List<RequirementConnections> requirementConnections;
        private List<String> missingEvidence;
        private String targetWeaknessTag;
        private String targetDimension;
        private List<Long> originEvaluationIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeaknessTagSummary {
        private String tag;
        private int count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NextPracticeRecommendation {
        private InterviewQuestionType questionType;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequirementConnections {
        private String requirement;
        private MatchAnalysisResultMatchLevel matchLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsedGuide {
        private Long guideId;
        private String version;
    }
}