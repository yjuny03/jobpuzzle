package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomizedSynthesisV18SchemaFactoryTest {
    private final CustomizedSynthesisV18SchemaFactory factory =
            new CustomizedSynthesisV18SchemaFactory(new ObjectMapper());

    @Test
    void usesQuestionArrayWhenGenerationIsEnabled() {
        var schema = factory.schema(input(true));

        assertThat(schema.at("/properties/questions/type").asText()).isEqualTo("array");
        assertThat(schema.at("/properties/questions/items/type").asText()).isEqualTo("object");
        assertThat(schema.at("/properties/questions/items/properties/relatedRequirementId/enum/0").asText())
                .isEqualTo("req-1");
        assertThat(schema.at("/properties/primaryQuestion").isMissingNode()).isTrue();
    }

    @Test
    void usesNullQuestionsWhenGenerationIsDisabled() {
        assertThat(factory.schema(input(false)).at("/properties/questions/type").asText())
                .isEqualTo("null");
    }

    private CustomizedSynthesisProviderInput input(boolean enabled) {
        return new CustomizedSynthesisProviderInput(
                null,
                List.of(new CustomizedSynthesisProviderInput.RequirementItem(
                        "req-1", RequirementType.REQUIRED, "Java 경험",
                        List.of("posting-1"), List.of("candidate-1"))),
                List.of(),
                null,
                new CustomizedSynthesisProviderEvidenceCatalog(List.of(
                        new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                                "posting-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING, "공고 근거"),
                        new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                                "candidate-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE, "지원자 근거"))),
                new CustomizedSynthesisProviderInput.GenerationPolicy(enabled, enabled ? 1 : 0));
    }
}
