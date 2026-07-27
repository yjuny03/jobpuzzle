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

class CustomizedSynthesisV17SchemaFactoryTest {
    private final CustomizedSynthesisV17SchemaFactory factory =
            new CustomizedSynthesisV17SchemaFactory(new ObjectMapper());

    @Test
    void embedsRequirementScopedEvidenceInCompactDecisionEnum() {
        JsonNode schema = factory.schema(input());

        String req1 = schema.at("/properties/decisionById/properties/req-1/enum").toString();
        String req2 = schema.at("/properties/decisionById/properties/req-2/enum").toString();
        assertThat(req1).contains("HIGH::cand-1", "NONE").doesNotContain("cand-2");
        assertThat(req2).contains("LOW::cand-2", "INSUFFICIENT").doesNotContain("cand-1");
        assertThat(schema.at("/properties/decisionById/required").toString()).contains("req-1", "req-2");
        assertThat(schema.at("/properties/narrativesById/required").toString()).contains("req-1", "req-2");
        assertThat(schema.toString()).doesNotContain("anyOf", "minItems", "minLength");
    }

    private CustomizedSynthesisProviderInput input() {
        return new CustomizedSynthesisProviderInput(null, List.of(
                new CustomizedSynthesisProviderInput.RequirementItem(
                        "req-1", RequirementType.REQUIRED, "Java", List.of("post-1"), List.of("cand-1")),
                new CustomizedSynthesisProviderInput.RequirementItem(
                        "req-2", RequirementType.PREFERRED, "Cloud", List.of("post-2"), List.of("cand-2"))
        ), List.of(), null, new CustomizedSynthesisProviderEvidenceCatalog(List.of(
                new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        "post-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING, "Java"),
                new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        "cand-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE, "Java 경험"),
                new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        "cand-2", CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE, "Cloud 경험")
        )), new CustomizedSynthesisProviderInput.GenerationPolicy(true, 1));
    }
}
