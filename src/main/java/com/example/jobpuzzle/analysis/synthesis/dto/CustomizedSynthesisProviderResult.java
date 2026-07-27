package com.example.jobpuzzle.analysis.synthesis.dto;

import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * JSON-05 v1.3 provider 전용 응답이다. source identity와 저장용 내부 ID는 포함하지 않는다.
 * 서버 assembler가 이 값을 권위 catalog와 결합해 기존 저장 DTO를 만든다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizedSynthesisProviderResult {

    private Readiness readiness;
    private List<RequirementMatch> requirementMatches;
    private List<Question> questions;
    private List<Task> tasks;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Readiness {
        private String reason;
        private List<String> limitations;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequirementMatch {
        private String requirementId;
        private MatchAnalysisResultMatchLevel matchLevel;
        private String reason;
        private String missingPoint;
        private String candidateEvidence;
        private List<String> candidateEvidenceIds;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        private String relatedRequirementId;
        private InterviewQuestionType questionType;
        private String question;
        private String intent;
        private List<InterviewQuestionEvaluationFocus> evaluationFocus;
        private List<String> evidenceIds;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Task {
        private String relatedRequirementId;
        private String missingPoint;
        private String suggestion;
    }
}
