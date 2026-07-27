package com.example.jobpuzzle.analysis.synthesis.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CustomizedSynthesisProviderResultContractTest {

    private static final Set<String> FORBIDDEN_PROVIDER_FIELDS = Set.of(
            "requirementType", "requirement", "requirementText",
            "postingSourceRefs", "candidateSourceRefs", "sourceRefs",
            "extractionId", "documentId", "documentType", "pageNumber", "segmentId", "evidenceText",
            "matchId", "questionId", "taskId", "relatedMatchId", "reviewStatus", "displayOrder");

    @Test
    void providerContractContainsNoServerOwnedIdentityOrSourceFields() {
        Set<String> fields = Set.of(
                        CustomizedSynthesisProviderResult.class,
                        CustomizedSynthesisProviderResult.Readiness.class,
                        CustomizedSynthesisProviderResult.RequirementMatch.class,
                        CustomizedSynthesisProviderResult.Question.class,
                        CustomizedSynthesisProviderResult.Task.class).stream()
                .flatMap(type -> java.util.Arrays.stream(type.getDeclaredFields()))
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).doesNotContainAnyElementsOf(FORBIDDEN_PROVIDER_FIELDS);
    }

    @Test
    void v15ProviderContractContainsNoServerOwnedIdentityOrSourceFields() {
        Set<String> fields = Set.of(
                        CustomizedSynthesisV15ProviderResult.class,
                        CustomizedSynthesisV15ProviderResult.Readiness.class,
                        CustomizedSynthesisV15ProviderResult.MatchSlot.class,
                        CustomizedSynthesisV15ProviderResult.QuestionSlot.class,
                        CustomizedSynthesisV15ProviderResult.TaskSlot.class).stream()
                .flatMap(type -> java.util.Arrays.stream(type.getDeclaredFields()))
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).doesNotContainAnyElementsOf(FORBIDDEN_PROVIDER_FIELDS);
    }

    @Test
    void v16ProviderContractContainsNoServerOwnedIdentityOrSourceFields() {
        Set<String> fields = Set.of(
                        CustomizedSynthesisV16ProviderResult.class,
                        CustomizedSynthesisV16ProviderResult.Readiness.class,
                        CustomizedSynthesisV16ProviderResult.QuestionSlot.class).stream()
                .flatMap(type -> java.util.Arrays.stream(type.getDeclaredFields()))
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).doesNotContainAnyElementsOf(FORBIDDEN_PROVIDER_FIELDS);
    }

    @Test
    void v17ProviderContractContainsNoServerOwnedIdentityOrSourceFields() {
        Set<String> fields = java.util.Arrays.stream(CustomizedSynthesisV17ProviderResult.class.getDeclaredFields())
                .map(Field::getName).collect(Collectors.toSet());
        assertThat(fields).doesNotContainAnyElementsOf(FORBIDDEN_PROVIDER_FIELDS);
    }
}
