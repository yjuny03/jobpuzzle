package com.example.jobpuzzle.analysis.synthesis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/** JSON-05 v1.7 compact wire response. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizedSynthesisV17ProviderResult {
    private CustomizedSynthesisV16ProviderResult.Readiness readiness;
    /** NONE|INSUFFICIENT 또는 HIGH::evidenceId|MEDIUM::evidenceId|LOW::evidenceId */
    private Map<String, String> decisionById;
    /** [reason, missingPoint, candidateEvidence, taskSuggestion] 순서의 정확히 네 문자열. */
    private Map<String, List<String>> narrativesById;
    private CustomizedSynthesisV16ProviderResult.QuestionSlot primaryQuestion;
}
