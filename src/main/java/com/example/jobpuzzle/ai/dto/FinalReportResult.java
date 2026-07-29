package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// JSON-07 FinalReportResult
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinalReportResult {

    private InterviewSessionMode interviewMode;

    private int totalQuestionCount;
    private int submittedQuestionCount;
    private int evaluatedQuestionCount;
    private int evaluationFailedQuestionCount;
    private int skippedQuestionCount;
    private int completionRate;

    // WEAKNESS_REVIEW는 null
    private Integer overallScore;
    private String scoreLabel;
    // 질문별 답변·평가를 종합한 총평 문단
    private String overallAssessment;

    private CategoryScores categoryScores;
    private CategoryScoreReasons categoryScoreReasons;
    private BasisSummary basisSummary;
    private List<WeaknessTagSummary> weaknessTagSummary;
    private List<NextPracticeRecommendation> nextPracticeRecommendation;
    private ImprovementSuggestion improvementSuggestion;
    private List<String> learningDirection;

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
        // COMPANY_FIT이 아니면 null
        private Integer requirementConnection;
        // 적용 가이드가 없거나 WEAKNESS_REVIEW의 targetDimension이 아니면 null
        private Integer guideAlignment;
        private Integer deliveryClarity;
    }

    // categoryScores에 값이 있는 관점에 대해서만 채우는 근거 서술
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BasisSummary {
        private String jobCategory;
        private JobCategoryCareerLevel careerLevel;
        private Integer evaluationPassThreshold;
        private UsedGuide usedGuide;
        // COMPANY_FIT 외 모드는 빈 배열
        private List<RequirementConnections> requirementConnections;
        // COMPANY_FIT 외 모드는 빈 배열
        private List<String> missingEvidence;
        // WEAKNESS_REVIEW 외 모드는 null
        private String targetWeaknessTag;
        // WEAKNESS_REVIEW 외 모드는 null
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
        // 이 태그가 어떤 답변의 score·summary 근거로 나왔는지에 대한 설명
        private String reason;
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
    public static class ImprovementSuggestion {
        private List<String> resume;
        private List<String> coverLetter;
        private List<String> portfolio;
        private List<String> experienceNote;
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