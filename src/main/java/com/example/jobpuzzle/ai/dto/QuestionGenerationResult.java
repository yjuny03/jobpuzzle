package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.analysis.entity.ActionPlanMatchLevel;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.analysis.entity.ReadinessResultStatus;
import com.example.jobpuzzle.interview.entity.InterviewQuestionReviewStatus;
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
public class QuestionGenerationResult {
    private Readiness readiness;
    private List<RequirementMatch> requirementMatches;
    private List<Question> questions;
    private List<ActionPlanItem> tasks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Readiness{
        private ReadinessResultStatus status;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequirementMatch{
        private String requirement;
        private String candidateEvidence;
        private MatchAnalysisResultMatchLevel matchLevel;
        private String reason;
        private String missingPoint;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Question{
        private String questionId;
        private InterviewQuestionType questionType;
        private String question;
        private String intent;
        private List<String> evaluationFocus;
        private String relatedRequirement;
        private InterviewQuestionReviewStatus reviewStatus;
        private String reviewNote;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActionPlanItem{
        private String taskId;
        private String relatedRequirement;
        private ActionPlanMatchLevel matchLevel;
        private String missingPoint;
        private String suggestion;
    }
}
