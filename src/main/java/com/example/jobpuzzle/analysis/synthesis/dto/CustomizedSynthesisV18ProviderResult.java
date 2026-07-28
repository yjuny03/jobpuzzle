package com.example.jobpuzzle.analysis.synthesis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/** JSON-05 v1.8: compact decision과 필수 narrative 객체를 결합한 wire response. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomizedSynthesisV18ProviderResult {
    private CustomizedSynthesisV16ProviderResult.Readiness readiness;
    private Map<String, String> decisionById;
    private Map<String, Narrative> narrativesById;
    private List<CustomizedSynthesisV16ProviderResult.QuestionSlot> questions;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Narrative {
        private String reason;
        private String missingPoint;
        private String candidateEvidence;
        private String taskSuggestion;
    }
}
