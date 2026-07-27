package com.example.jobpuzzle.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomizedSynthesisSchemaFactoryTest {

    private final CustomizedSynthesisSchemaFactory factory = new CustomizedSynthesisSchemaFactory(new ObjectMapper());

    @Test
    void matchesTheExistingJson05DtoShapeWithoutChangingTheQuestionSetBoundary() {
        var schema = factory.schema();

        assertThat(schema.at("/additionalProperties").asBoolean()).isFalse();
        assertThat(schema.at("/required").toString()).contains("readiness", "evidencedRequirementMatches",
                "nonEvidencedRequirementMatches", "questions", "tasks").doesNotContain("requirementMatches\"");
        assertThat(schema.at("/properties/nonEvidencedRequirementMatches/items/properties/candidateEvidence").isMissingNode()).isTrue();
        assertThat(schema.at("/properties/evidencedRequirementMatches/items/properties/candidateEvidenceIds/minItems").asInt()).isEqualTo(1);
        assertThat(schema.at("/properties/evidencedRequirementMatches/items/properties/candidateSourceRefs").isMissingNode()).isTrue();
        assertThat(schema.at("/properties/readiness/properties/reason/minLength").asInt()).isEqualTo(1);
        assertThat(schema.at("/properties/readiness/properties/limitations/items/minLength").asInt()).isEqualTo(1);
        assertThat(schema.at("/properties/evidencedRequirementMatches/items/properties/postingSourceRefs/minItems").asInt()).isEqualTo(1);
        assertThat(schema.at("/properties/questions/items/properties/evaluationFocus/minItems").asInt()).isEqualTo(1);
        assertThat(schema.at("/properties/questions/items/properties/sourceRefs/minItems").asInt()).isEqualTo(1);
        assertThat(schema.at("/properties/tasks/items/properties/suggestion/minLength").asInt()).isEqualTo(1);
        assertThat(schema.at("/properties/questions/items/properties/reviewStatus/enum/0").asText()).isEqualTo("PASS");
        assertThat(schema.at("/properties/questions/items/properties/sourceRefs/items/$ref").asText())
                .isEqualTo("#/$defs/sourceReference");
        assertThat(schema.at("/$defs/sourceReference/properties/evidenceText/type").asText()).isEqualTo("string");
    }

    @Test
    void derivesAllowedRequirementIdsFromRenderedRetrievalEvidenceWithoutUnsupportedSchemaCombinators() {
        var schema = factory.schemaForPrompt("""
                <RETRIEVED_EVIDENCE>{"requirements":[{"requirementId":"req-1"},{"requirementId":"req-2"},{"requirementId":"req-3"}]}</RETRIEVED_EVIDENCE>
                """);

        assertThat(schema.at("/properties/evidencedRequirementMatches/items/properties/requirementId/enum").toString())
                .contains("req-1", "req-2", "req-3");
        assertThat(schema.has("anyOf")).isFalse();
    }

    @Test
    void derivesAllowedCandidateEvidenceIdsFromRenderedRetrievalEvidence() {
        var schema = factory.schemaForPrompt("""
                <RETRIEVED_EVIDENCE>{"requirements":[],"candidateEvidence":[{"evidenceId":"chunk-101"},{"evidenceId":"chunk-102"}]}</RETRIEVED_EVIDENCE>
                """);

        assertThat(schema.at("/properties/evidencedRequirementMatches/items/properties/candidateEvidenceIds/items/enum").toString())
                .contains("chunk-101", "chunk-102");
    }
}
