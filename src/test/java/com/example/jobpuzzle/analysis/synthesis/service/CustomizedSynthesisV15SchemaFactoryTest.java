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

class CustomizedSynthesisV15SchemaFactoryTest {
    private final CustomizedSynthesisV15SchemaFactory factory =
            new CustomizedSynthesisV15SchemaFactory(new ObjectMapper());

    @Test
    void fixesEveryRequirementIntoRequiredMatchAndTaskSlots() {
        JsonNode schema = factory.schema(input(true));

        assertThat(schema.at("/properties/requirementMatchesById/required").toString())
                .contains("req-1", "req-2");
        assertThat(schema.at("/properties/tasksByRequirementId/required").toString())
                .contains("req-1", "req-2");
        assertThat(schema.at("/properties/requirementMatchesById/additionalProperties").asBoolean()).isFalse();
        assertThat(schema.at("/properties/tasksByRequirementId/additionalProperties").asBoolean()).isFalse();
        assertThat(schema.at("/properties/requirementMatchesById/properties/req-1/$ref").asText())
                .isEqualTo("#/$defs/matchSlot");
        assertThat(schema.at("/properties/tasksByRequirementId/properties/req-1/$ref").asText())
                .isEqualTo("#/$defs/taskSlot");
        assertThat(schema.at("/$defs/matchSlot/type").asText()).isEqualTo("object");
        assertThat(schema.at("/properties/primaryQuestion/type").asText()).isEqualTo("object");
        assertThat(schema.toString()).doesNotContain("anyOf", "minItems", "minLength");
    }

    @Test
    void usesNullQuestionSchemaWhenServerDisablesGeneration() {
        assertThat(factory.schema(input(false)).at("/properties/primaryQuestion/type").asText())
                .isEqualTo("null");
    }

    private CustomizedSynthesisProviderInput input(boolean questions) {
        return new CustomizedSynthesisProviderInput(
                new CustomizedSynthesisProviderInput.JobContext("IT", "BACKEND", "NEW"),
                List.of(
                        new CustomizedSynthesisProviderInput.RequirementItem(
                                "req-1", RequirementType.REQUIRED, "Java", List.of("post-1"), List.of("cand-1")),
                        new CustomizedSynthesisProviderInput.RequirementItem(
                                "req-2", RequirementType.PREFERRED, "Cloud", List.of("post-2"), List.of())),
                List.of(), null,
                new CustomizedSynthesisProviderEvidenceCatalog(List.of(
                        new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                                "post-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING, "Java"),
                        new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                                "post-2", CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING, "Cloud"),
                        new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                                "cand-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE, "Java 경험"))),
                new CustomizedSynthesisProviderInput.GenerationPolicy(questions, questions ? 1 : 0));
    }
}
