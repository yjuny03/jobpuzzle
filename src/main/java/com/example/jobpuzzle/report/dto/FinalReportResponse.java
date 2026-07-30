package com.example.jobpuzzle.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// JSON-07 스키마를 그대로 따르는 조회 응답
@Getter
@Builder
public class FinalReportResponse {

    private Long sessionId;
    private String interviewMode;
    private Long analysisCaseId;

    private int totalQuestionCount;
    private int submittedQuestionCount;
    private int evaluatedQuestionCount;
    private int evaluationFailedQuestionCount;
    private int skippedQuestionCount;
    private int completionRate;

    // WEAKNESS_REVIEW는 overallScore/scoreLabel이 없다는 게 문서 스펙이지만,
    // FinalReport 엔티티 컬럼이 현재 NOT NULL이라 이 필드는 항상 채워짐.
    private Integer overallScore;
    private String scoreLabel;
    private String overallAssessment;

    private CategoryScores categoryScores;
    private CategoryScoreReasons categoryScoreReasons;
    private BasisSummary basisSummary;
    private List<WeaknessTagSummary> weaknessTagSummary;
    private List<NextPracticeRecommendation> nextPracticeRecommendation;
    private ImprovementSuggestion improvementSuggestion;
    private List<String> learningDirection;

    @Getter
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

    // 관점별 점수가 왜 그렇게 나왔는지에 대한 AI 서술. 값이 없는 관점은 null.
    @Getter
    @Builder
    public static class CategoryScoreReasons {
        private String intentMatch;
        private String specificity;
        private String ownRole;
        private String problemSolving;
        private String resultExpression;
        private String requirementConnection;
        private String guideAlignment;
        private String deliveryClarity;
    }

    @Getter
    @Builder
    public static class BasisSummary {
        private String jobCategory;
        private String careerLevel;
        private Integer evaluationPassThreshold;
        private UsedGuide usedGuide;
        private List<RequirementConnection> requirementConnections;
        private List<String> missingEvidence;
        private String targetWeaknessTag;
        private String targetDimension;
        private List<Long> originEvaluationIds;
    }

    @Getter
    @Builder
    public static class UsedGuide {
        private Long guideId;
        private String version;
    }

    @Getter
    @Builder
    public static class RequirementConnection {
        private String requirement;
        private String matchLevel;
    }

    @Getter
    @Builder
    public static class WeaknessTagSummary {
        private String tag;
        private int count;
        private String reason;
    }

    @Getter
    @Builder
    public static class NextPracticeRecommendation {
        private String questionType;
        private String reason;
    }

    @Getter
    @Builder
    public static class ImprovementSuggestion {
        private List<String> resume;
        private List<String> coverLetter;
        private List<String> portfolio;
        private List<String> experienceNote;
    }
}