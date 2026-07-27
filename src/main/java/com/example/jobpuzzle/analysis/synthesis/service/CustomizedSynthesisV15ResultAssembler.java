package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderInput;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderResult;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisV15ProviderResult;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** v1.5 고정 슬롯 응답을 기존 저장 계약으로 변환한다. */
@Component
public class CustomizedSynthesisV15ResultAssembler {
    private final CustomizedSynthesisResultAssembler legacyAssembler;

    public CustomizedSynthesisV15ResultAssembler(CustomizedSynthesisResultAssembler legacyAssembler) {
        this.legacyAssembler = legacyAssembler;
    }

    public CustomizedAnalysisGenerationResult assemble(CustomizedSynthesisV15ProviderResult provider,
                                                        CustomizedSynthesisProviderInput input,
                                                        CustomizedSynthesisEvidenceCatalog authority,
                                                        GuideMatchType guideMatchType) {
        if (provider == null || provider.getReadiness() == null || input == null
                || input.generationPolicy() == null || authority == null) {
            fail("v1.5 provider result, policy, and authority are required");
        }
        Set<String> expected = new LinkedHashSet<>();
        authority.requirements().forEach(value -> expected.add(value.requirementId()));
        exactKeys("requirementMatchesById", provider.getRequirementMatchesById(), expected);
        exactKeys("tasksByRequirementId", provider.getTasksByRequirementId(), expected);

        List<CustomizedSynthesisProviderResult.RequirementMatch> matches = new ArrayList<>();
        List<CustomizedSynthesisProviderResult.Task> tasks = new ArrayList<>();
        for (String requirementId : expected) {
            CustomizedSynthesisV15ProviderResult.MatchSlot match =
                    provider.getRequirementMatchesById().get(requirementId);
            CustomizedSynthesisV15ProviderResult.TaskSlot task =
                    provider.getTasksByRequirementId().get(requirementId);
            if (match == null || task == null) fail("v1.5 requirement slots must not be null");
            validateTask(requirementId, match.getMatchLevel(), task);
            matches.add(CustomizedSynthesisProviderResult.RequirementMatch.builder()
                    .requirementId(requirementId)
                    .matchLevel(match.getMatchLevel())
                    .reason(match.getReason())
                    .missingPoint(emptyToNull(match.getMissingPoint()))
                    .candidateEvidence(emptyToNull(match.getCandidateEvidence()))
                    .candidateEvidenceIds(match.getCandidateEvidenceIds())
                    .build());
            if (task.isApplicable()) {
                tasks.add(CustomizedSynthesisProviderResult.Task.builder()
                        .relatedRequirementId(requirementId)
                        .missingPoint(task.getMissingPoint())
                        .suggestion(task.getSuggestion())
                        .build());
            }
        }

        boolean questionsEnabled = input.generationPolicy().questionGenerationEnabled();
        CustomizedSynthesisV15ProviderResult.QuestionSlot question = provider.getPrimaryQuestion();
        if (questionsEnabled != (question != null)) {
            fail(questionsEnabled
                    ? "primaryQuestion is required by generation policy"
                    : "primaryQuestion must be null when generation is disabled");
        }
        List<CustomizedSynthesisProviderResult.Question> questions = question == null ? List.of() : List.of(
                CustomizedSynthesisProviderResult.Question.builder()
                        .relatedRequirementId(emptyToNull(question.getRelatedRequirementId()))
                        .questionType(question.getQuestionType())
                        .question(question.getQuestion())
                        .intent(question.getIntent())
                        .evaluationFocus(question.getEvaluationFocus())
                        .evidenceIds(question.getEvidenceIds())
                        .build());
        CustomizedSynthesisProviderResult legacy = CustomizedSynthesisProviderResult.builder()
                .readiness(CustomizedSynthesisProviderResult.Readiness.builder()
                        .reason(provider.getReadiness().getReason())
                        .limitations(provider.getReadiness().getLimitations())
                        .build())
                .requirementMatches(matches)
                .questions(questions)
                .tasks(tasks)
                .build();
        return legacyAssembler.assemble(legacy, authority, guideMatchType);
    }

    private void validateTask(String requirementId, MatchAnalysisResultMatchLevel level,
                              CustomizedSynthesisV15ProviderResult.TaskSlot task) {
        if (level == null) fail("matchLevel is required for " + requirementId);
        if (level == MatchAnalysisResultMatchLevel.HIGH) {
            if (task.isApplicable() || !blank(task.getMissingPoint()) || !blank(task.getSuggestion())) {
                fail("HIGH task slot must be inapplicable and blank");
            }
        } else if (!task.isApplicable() || blank(task.getMissingPoint()) || blank(task.getSuggestion())) {
            fail("non-HIGH task slot must be applicable and complete");
        }
    }

    private void exactKeys(String field, Map<String, ?> slots, Set<String> expected) {
        if (slots == null || !slots.keySet().equals(expected)) {
            fail(field + " must contain every requirement exactly once and no unknown key");
        }
    }

    private String emptyToNull(String value) {
        return blank(value) ? null : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void fail(String message) {
        throw new AiProcessingException(AiCallLogErrorType.RESPONSE_VALIDATION_FAILED, message);
    }
}
