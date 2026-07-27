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

/** JSON-05 v1.6 provider 응답. requirement별 상관관계를 평탄 map의 동일 key로 고정한다. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizedSynthesisV16ProviderResult {
    private Readiness readiness;
    private Map<String, MatchAnalysisResultMatchLevel> matchLevelsById;
    private Map<String, String> matchReasonsById;
    private Map<String, String> missingPointsById;
    private Map<String, String> candidateEvidenceById;
    /** 빈 문자열은 선택 없음, 그 외에는 schema가 requirement별 허용 ID 하나로 제한한다. */
    private Map<String, String> candidateEvidenceIdById;
    private QuestionSlot primaryQuestion;
    private Map<String, Boolean> taskApplicableById;
    private Map<String, String> taskMissingPointsById;
    private Map<String, String> taskSuggestionsById;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Readiness {
        private String reason;
        private List<String> limitations;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class QuestionSlot {
        private String relatedRequirementId;
        private InterviewQuestionType questionType;
        private String question;
        private String intent;
        private InterviewQuestionEvaluationFocus evaluationFocus;
        private String evidenceId;
    }
}
