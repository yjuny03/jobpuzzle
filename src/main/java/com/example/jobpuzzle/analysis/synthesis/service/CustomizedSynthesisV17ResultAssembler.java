package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.analysis.synthesis.dto.*;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** compact wire 값을 v1.5 의미 계약으로 복원한다. */
@Component
public class CustomizedSynthesisV17ResultAssembler {
    private final CustomizedSynthesisV15ResultAssembler v15Assembler;

    public CustomizedSynthesisV17ResultAssembler(CustomizedSynthesisV15ResultAssembler v15Assembler) {
        this.v15Assembler = v15Assembler;
    }

    public CustomizedAnalysisGenerationResult assemble(CustomizedSynthesisV17ProviderResult provider,
                                                        CustomizedSynthesisProviderInput input,
                                                        CustomizedSynthesisEvidenceCatalog authority,
                                                        GuideMatchType guideMatchType) {
        if (provider == null || provider.getReadiness() == null || input == null || authority == null) {
            fail("v1.7 provider result and authority are required");
        }
        Set<String> expected = authority.requirements().stream()
                .map(CustomizedSynthesisEvidenceCatalog.RequirementItem::requirementId)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        exact("decisionById", provider.getDecisionById(), expected);
        exact("narrativesById", provider.getNarrativesById(), expected);
        Map<String, CustomizedSynthesisV15ProviderResult.MatchSlot> matches = new LinkedHashMap<>();
        Map<String, CustomizedSynthesisV15ProviderResult.TaskSlot> tasks = new LinkedHashMap<>();
        for (String id : expected) {
            Decision decision = decision(provider.getDecisionById().get(id));
            List<String> narrative = provider.getNarrativesById().get(id);
            if (narrative == null || narrative.size() != 4 || narrative.stream().anyMatch(java.util.Objects::isNull)) {
                fail("narrativesById values must contain exactly four strings");
            }
            String reason = narrative.get(0);
            String missing = narrative.get(1);
            String candidateEvidence = narrative.get(2);
            String suggestion = narrative.get(3);
            matches.put(id, new CustomizedSynthesisV15ProviderResult.MatchSlot(
                    decision.level(), reason, missing, candidateEvidence,
                    decision.evidenceId() == null ? List.of() : List.of(decision.evidenceId())));
            boolean applicable = decision.level() != MatchAnalysisResultMatchLevel.HIGH;
            tasks.put(id, new CustomizedSynthesisV15ProviderResult.TaskSlot(
                    applicable, applicable ? missing : "", applicable ? suggestion : ""));
        }
        CustomizedSynthesisV16ProviderResult.QuestionSlot question = provider.getPrimaryQuestion();
        CustomizedSynthesisV15ProviderResult.QuestionSlot restoredQuestion = question == null ? null
                : new CustomizedSynthesisV15ProviderResult.QuestionSlot(
                question.getRelatedRequirementId(), question.getQuestionType(), question.getQuestion(),
                question.getIntent(),
                question.getEvaluationFocus() == null ? null : List.of(question.getEvaluationFocus()),
                blank(question.getEvidenceId()) ? List.of() : List.of(question.getEvidenceId()));
        CustomizedSynthesisV15ProviderResult restored = new CustomizedSynthesisV15ProviderResult(
                new CustomizedSynthesisV15ProviderResult.Readiness(
                        provider.getReadiness().getReason(), provider.getReadiness().getLimitations()),
                matches, restoredQuestion, tasks);
        return v15Assembler.assemble(restored, input, authority, guideMatchType);
    }

    private Decision decision(String value) {
        if ("NONE".equals(value)) return new Decision(MatchAnalysisResultMatchLevel.NONE, null);
        if ("INSUFFICIENT".equals(value)) return new Decision(MatchAnalysisResultMatchLevel.INSUFFICIENT, null);
        if (value != null) {
            String[] parts = value.split("::", 2);
            if (parts.length == 2 && !blank(parts[1])) {
                try {
                    MatchAnalysisResultMatchLevel level = MatchAnalysisResultMatchLevel.valueOf(parts[0]);
                    if (Set.of(MatchAnalysisResultMatchLevel.HIGH,
                            MatchAnalysisResultMatchLevel.MEDIUM,
                            MatchAnalysisResultMatchLevel.LOW).contains(level)) {
                        return new Decision(level, parts[1]);
                    }
                } catch (IllegalArgumentException ignored) {
                    // 아래 고정 계약 오류로 통합한다.
                }
            }
        }
        fail("decisionById value is invalid");
        return null;
    }

    private void exact(String field, Map<String, ?> values, Set<String> expected) {
        if (values == null || !values.keySet().equals(expected)) {
            fail(field + " must contain every requirement exactly once and no unknown key");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void fail(String message) {
        throw new AiProcessingException(AiCallLogErrorType.RESPONSE_VALIDATION_FAILED, message);
    }

    private record Decision(MatchAnalysisResultMatchLevel level, String evidenceId) {
    }
}
