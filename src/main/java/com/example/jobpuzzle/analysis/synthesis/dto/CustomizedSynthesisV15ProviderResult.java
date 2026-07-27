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
import java.util.Map;

/**
 * JSON-05 v1.5 provider 전용 응답.
 * 배열 길이와 nullable 상관관계를 모델에게 맡기지 않고 requirement별 필수 슬롯으로 받는다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizedSynthesisV15ProviderResult {
    private Readiness readiness;
    private Map<String, MatchSlot> requirementMatchesById;
    private QuestionSlot primaryQuestion;
    private Map<String, TaskSlot> tasksByRequirementId;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Readiness {
        private String reason;
        private List<String> limitations;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MatchSlot {
        private MatchAnalysisResultMatchLevel matchLevel;
        private String reason;
        private String missingPoint;
        private String candidateEvidence;
        private List<String> candidateEvidenceIds;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuestionSlot {
        /** 빈 문자열은 일반 질문이며 서버가 null로 복원한다. */
        private String relatedRequirementId;
        private InterviewQuestionType questionType;
        private String question;
        private String intent;
        private List<InterviewQuestionEvaluationFocus> evaluationFocus;
        private List<String> evidenceIds;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TaskSlot {
        private boolean applicable;
        private String missingPoint;
        private String suggestion;
    }
}
