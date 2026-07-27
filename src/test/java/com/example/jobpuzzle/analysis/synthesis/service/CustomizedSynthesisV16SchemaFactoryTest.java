package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomizedSynthesisV16SchemaFactoryTest {
    private final CustomizedSynthesisV16SchemaFactory factory =
            new CustomizedSynthesisV16SchemaFactory(new ObjectMapper());

    @Test
    void candidateIdEnumIsDifferentForEachRequirement() {
        JsonNode schema = factory.schema(input(true));

        assertThat(schema.at("/properties/candidateEvidenceIdById/properties/req-1/enum").toString())
                .contains("\"\"", "cand-1").doesNotContain("cand-2");
        assertThat(schema.at("/properties/candidateEvidenceIdById/properties/req-2/enum").toString())
                .contains("\"\"", "cand-2").doesNotContain("cand-1");
        assertThat(schema.at("/properties/matchLevelsById/required").toString())
                .contains("req-1", "req-2");
        assertThat(schema.at("/properties/primaryQuestion/properties/relatedRequirementId/enum").toString())
                .isEqualTo("[\"\"]");
        assertThat(schema.toString()).doesNotContain("anyOf", "minItems", "minLength");
    }

    private CustomizedSynthesisProviderInput input(boolean questions) {
        return new CustomizedSynthesisProviderInput(null, List.of(
                new CustomizedSynthesisProviderInput.RequirementItem(
                        "req-1", RequirementType.REQUIRED, "Java", List.of("post-1"), List.of("cand-1")),
                new CustomizedSynthesisProviderInput.RequirementItem(
                        "req-2", RequirementType.PREFERRED, "Cloud", List.of("post-2"), List.of("cand-2"))
        ), List.of(), null, new CustomizedSynthesisProviderEvidenceCatalog(List.of(
                new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        "post-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING, "Java"),
                new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        "post-2", CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING, "Cloud"),
                new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        "cand-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE, "Java 경험"),
                new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        "cand-2", CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE, "Cloud 경험")
        )), new CustomizedSynthesisProviderInput.GenerationPolicy(questions, questions ? 1 : 0));
    }
}
