package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisSchemaContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomizedSynthesisV13SchemaFactoryTest {
    @Test
    void schemaMatchesProviderDtoAndUsesOnlyServerValidatedIds() {
        var schema = new CustomizedSynthesisV13SchemaFactory(new ObjectMapper())
                .schema(new CustomizedSynthesisSchemaContext(
                        List.of("req-1", "req-2"), List.of("posting-1"), List.of("candidate-1")));
        String serialized = schema.toString();

        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        var requirementIds = schema.at("/properties/requirementMatches/items/properties/requirementId/enum");
        assertThat(List.of(requirementIds.get(0).asText(), requirementIds.get(1).asText()))
                .containsExactly("req-1", "req-2");
        assertThat(serialized)
                .doesNotContain("sourceRefs", "extractionId", "documentId", "matchId", "questionId", "taskId")
                .doesNotContain("minLength", "minItems", "anyOf", "oneOf");
        assertThat(serialized)
                .contains("\"candidateEvidenceIds\":{\"items\":{\"enum\":[\"candidate-1\"]");
    }

    @Test
    void permitsPostingLackSchemaWhileAssemblerStillOwnsExactSetValidation() {
        var schema = new CustomizedSynthesisV13SchemaFactory(new ObjectMapper())
                .schema(new CustomizedSynthesisSchemaContext(List.of(), List.of(), List.of()));

        assertThat(schema.at("/properties/requirementMatches/items/properties/requirementId/type").asText())
                .isEqualTo("string");
        assertThat(schema.at("/properties/questions/items/properties/relatedRequirementId/enum").get(0).isNull())
                .isTrue();
    }
}
