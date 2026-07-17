package com.example.jobpuzzle.ai.dto;


import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinalReportResult {

    private int overallScore;
    private String scoreLabel;
    private CategoryScores categoryScores;
    private EvidenceSummary evidenceSummary;
    private List<WeaknessTagSummary> weaknessTagSummaries;
    private List<NextPracticeRecommendation> nextPracticeRecommendations;
    private ImprovementSuggestion improvementSuggestion;
    private List<String> learningDirections;

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
