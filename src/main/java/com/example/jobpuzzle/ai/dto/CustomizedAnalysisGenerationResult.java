package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.analysis.entity.ActionPlanMatchLevel;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.analysis.entity.ReadinessResultStatus;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import com.example.jobpuzzle.interview.entity.InterviewQuestionReviewStatus;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// JSON-05 AI 응답 계약과 DB 저장 모델을 분리한 전용 DTO다.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizedAnalysisGenerationResult {
    private Readiness readiness;
    private List<RequirementMatch> requirementMatches;
    private List<Question> questions;
    private List<Task> tasks;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Readiness {
        private ReadinessResultStatus status;
        private boolean canGenerateQuestions;
        private String reason;
        private List<String> limitations;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RequirementMatch {
        private String matchId;
        private String requirementId;
        private RequirementType requirementType;
        private String requirement;
        private List<SourceReference> postingSourceRefs;
        private String candidateEvidence;
        private List<SourceReference> candidateSourceRefs;
        private MatchAnalysisResultMatchLevel matchLevel;
        private String reason;
        private String missingPoint;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Question {
        private String questionId;
        private InterviewQuestionType questionType;
        private String question;
        private String intent;
        private List<InterviewQuestionEvaluationFocus> evaluationFocus;
        private String relatedMatchId;
        private String relatedRequirementId;
        private List<SourceReference> sourceRefs;
        private InterviewQuestionReviewStatus reviewStatus;
        private String reviewNote;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Task {
        private String taskId;
        private String relatedMatchId;
        private String relatedRequirementId;
        private ActionPlanMatchLevel matchLevel;
        private String missingPoint;
        private String suggestion;
    }
}
