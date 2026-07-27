package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Json05V11PromptContractTest {

    @Test
    void separatesEvidenceBearingAndNonEvidenceRequirementMatches() throws Exception {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/json-05-v1.1.txt"));

        assertThat(prompt)
                .contains("evidencedRequirementMatches", "nonEvidencedRequirementMatches")
                .contains("HIGH, MEDIUM, LOW만", "candidateEvidence와 candidateSourceRefs를 포함")
                .contains("NONE, INSUFFICIENT만", "필드를 절대로 넣지 않는다")
                .contains("두 배열을 합쳤을 때만 전체 requirement 집합이 된다.");
    }

    @Test
    void v12UsesDeduplicatedCandidateEvidenceReferencesWithoutRelaxingSourceRules() throws Exception {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/json-05-v1.2.txt"));

        assertThat(prompt).contains("requirements와 candidateEvidence로 정규화", "candidateEvidenceIds",
                "candidateEvidence[].evidenceId", "같은 sourceRef를 중복 반환하지 않는다");
    }
}
