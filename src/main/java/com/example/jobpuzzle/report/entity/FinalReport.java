package com.example.jobpuzzle.report.entity;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import com.example.jobpuzzle.interview.entity.InterviewSession;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "final_report")
public class FinalReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "score_label", nullable = false)
    private String scoreLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_scores", columnDefinition = "json")
    private CategoryScores categoryScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_summary", columnDefinition = "json")
    private EvidenceSummary evidenceSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weakness_tag_summary", columnDefinition = "json")
    private List<WeaknessTagSummary> weaknessTagSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_practice_recommendation", columnDefinition = "json")
    private List<NextPracticeRecommendation> nextPracticeRecommendation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "learning_direction", columnDefinition = "json")
    private List<String> learningDirection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id")
    private AiCallLog aiCallLog;

    @Builder
    public FinalReport(
            InterviewSession session,
            int overallScore,
            String scoreLabel,
            CategoryScores categoryScores,
            EvidenceSummary evidenceSummary,
            List<WeaknessTagSummary> weaknessTagSummary,
            List<NextPracticeRecommendation> nextPracticeRecommendation,
            List<String> learningDirection,
            AiCallLog aiCallLog
    ) {
        this.session = session;
        this.overallScore = overallScore;
        this.scoreLabel = scoreLabel;
        this.categoryScores = categoryScores;
        this.evidenceSummary = evidenceSummary;
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
        private int companyRequirementFit;
        private int experienceSpecificity;
        private int roleClarity;
        private int problemSolving;
        private int resultExpression;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvidenceSummary {
        private List<RequirementConnections> requirementConnections;
        private UsedGuide usedGuide;
        private List<String> missingEvidence;
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
