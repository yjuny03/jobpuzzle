package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderInput;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisV15ProviderResult;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisV16ProviderResult;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** v1.6 평탄 map을 v1.5 의미 계약으로 복원한 뒤 기존 downstream assembler를 재사용한다. */
@Component
public class CustomizedSynthesisV16ResultAssembler {
    private final CustomizedSynthesisV15ResultAssembler v15Assembler;

    public CustomizedSynthesisV16ResultAssembler(CustomizedSynthesisV15ResultAssembler v15Assembler) {
        this.v15Assembler = v15Assembler;
    }

    public CustomizedAnalysisGenerationResult assemble(CustomizedSynthesisV16ProviderResult provider,
                                                        CustomizedSynthesisProviderInput input,
                                                        CustomizedSynthesisEvidenceCatalog authority,
                                                        GuideMatchType guideMatchType) {
        if (provider == null || provider.getReadiness() == null || input == null || authority == null) {
            fail("v1.6 provider result and authority are required");
        }
        Set<String> expected = new java.util.LinkedHashSet<>();
        authority.requirements().forEach(value -> expected.add(value.requirementId()));
        exact("matchLevelsById", provider.getMatchLevelsById(), expected);
        exact("matchReasonsById", provider.getMatchReasonsById(), expected);
        exact("missingPointsById", provider.getMissingPointsById(), expected);
        exact("candidateEvidenceById", provider.getCandidateEvidenceById(), expected);
        exact("candidateEvidenceIdById", provider.getCandidateEvidenceIdById(), expected);
        exact("taskApplicableById", provider.getTaskApplicableById(), expected);
        exact("taskMissingPointsById", provider.getTaskMissingPointsById(), expected);
        exact("taskSuggestionsById", provider.getTaskSuggestionsById(), expected);

        Map<String, CustomizedSynthesisV15ProviderResult.MatchSlot> matches = new LinkedHashMap<>();
        Map<String, CustomizedSynthesisV15ProviderResult.TaskSlot> tasks = new LinkedHashMap<>();
        for (String id : expected) {
            String evidenceId = provider.getCandidateEvidenceIdById().get(id);
            matches.put(id, new CustomizedSynthesisV15ProviderResult.MatchSlot(
                    provider.getMatchLevelsById().get(id), provider.getMatchReasonsById().get(id),
                    provider.getMissingPointsById().get(id), provider.getCandidateEvidenceById().get(id),
                    blank(evidenceId) ? List.of() : List.of(evidenceId)));
            Boolean applicable = provider.getTaskApplicableById().get(id);
            if (applicable == null) fail("taskApplicableById values must not be null");
            tasks.put(id, new CustomizedSynthesisV15ProviderResult.TaskSlot(
                    applicable, provider.getTaskMissingPointsById().get(id),
                    provider.getTaskSuggestionsById().get(id)));
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
}
