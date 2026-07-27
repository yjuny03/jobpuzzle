package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CandidateMaterialPartitionSchemaFactoryTest {
    private final CandidateMaterialPartitionSchemaFactory factory = new CandidateMaterialPartitionSchemaFactory(new ObjectMapper());

    @Test
    void forcesExactlyOneDocumentObjectAndStableSchemaFingerprintForEachType() {
        for (UserDocumentType type : new UserDocumentType[]{UserDocumentType.RESUME, UserDocumentType.COVER_LETTER, UserDocumentType.PORTFOLIO, UserDocumentType.EXPERIENCE_NOTE}) {
            var schema = factory.schema(type);
            assertThat(schema.at("/additionalProperties").asBoolean()).isFalse();
            assertThat(schema.at("/properties/availableDocumentTypes/items/enum/0").asText()).isEqualTo(type.name());
            assertThat(schema.at("/properties/" + objectName(type) + "/type").asText()).isEqualTo("object");
            assertThat(schema.toString()).doesNotContain("evidenceText");
            assertThat(factory.fingerprintMaterial(type)).isEqualTo(factory.fingerprintMaterial(type));
        }
    }

    @Test
    void excludesModelGeneratedTechnicalIdsFromStructuredOutputSchema() {
        var resume = factory.schema(UserDocumentType.RESUME);
        var portfolio = factory.schema(UserDocumentType.PORTFOLIO);
        var note = factory.schema(UserDocumentType.EXPERIENCE_NOTE);

        assertThat(resume.at("/properties/resume/properties/experiences/items/properties/experienceId").isMissingNode()).isTrue();
        assertThat(portfolio.at("/properties/portfolio/properties/projects/items/properties/projectId").isMissingNode()).isTrue();
        assertThat(note.at("/properties/experienceNote/properties/starCandidates/items/properties/candidateId").isMissingNode()).isTrue();
        assertThat(resume.at("/properties/resume/properties/experiences/items/required"))
                .extracting(node -> node.toString()).asString()
                .contains("sourceRef", "additionalSourceRefs").doesNotContain("sourceRefs");
        assertThat(resume.at("/$defs/sourceReference/type").asText()).isEqualTo("object");
        assertThat(resume.at("/properties/resume/properties/experiences/items/properties/sourceRef/$ref").asText())
                .isEqualTo("#/$defs/sourceReference");
        assertThat(CandidateMaterialPartitionSchemaFactory.SCHEMA_VERSION).isEqualTo("json02-partition-schema-v4");
    }

    private String objectName(UserDocumentType type) {
        return switch (type) {
            case RESUME -> "resume";
            case COVER_LETTER -> "coverLetter";
            case PORTFOLIO -> "portfolio";
            case EXPERIENCE_NOTE -> "experienceNote";
            default -> throw new IllegalArgumentException();
        };
    }
}
