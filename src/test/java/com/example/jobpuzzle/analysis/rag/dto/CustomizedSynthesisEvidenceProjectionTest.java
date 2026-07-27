package com.example.jobpuzzle.analysis.rag.dto;

import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalStatus;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomizedSynthesisEvidenceProjectionTest {

    @Test
    void sendsRepeatedChunkTextOnceAndPreservesEachRequirementRelationship() {
        RetrievedEvidenceContextDto.RetrievedChunk shared = chunk(101L, "공통 Spring 프로젝트 근거");
        RetrievedEvidenceContextDto source = new RetrievedEvidenceContextDto(List.of(
                requirement("req-1", List.of(shared, chunk(102L, "첫 번째 전용 근거"))),
                requirement("req-2", List.of(shared))
        ));

        CustomizedSynthesisEvidenceProjection projection = CustomizedSynthesisEvidenceProjection.from(source);

        assertThat(projection.candidateEvidence()).extracting(CustomizedSynthesisEvidenceProjection.CandidateEvidence::evidenceId)
                .containsExactly("chunk-101", "chunk-102");
        assertThat(projection.candidateEvidence().get(0)).satisfies(evidence -> {
            assertThat(evidence.content()).isEqualTo("공통 Spring 프로젝트 근거");
            assertThat(evidence.extractionId()).isEqualTo(2L);
            assertThat(evidence.documentId()).isEqualTo(20L);
            assertThat(evidence.documentType()).isEqualTo(UserDocumentType.RESUME);
            assertThat(evidence.pageStart()).isEqualTo(1);
        });
        assertThat(projection.requirements()).extracting(CustomizedSynthesisEvidenceProjection.RequirementEvidence::candidateEvidenceIds)
                .containsExactly(List.of("chunk-101", "chunk-102"), List.of("chunk-101"));
    }

    private RetrievedEvidenceContextDto.RequirementEvidence requirement(String requirementId,
                                                                          List<RetrievedEvidenceContextDto.RetrievedChunk> chunks) {
        return new RetrievedEvidenceContextDto.RequirementEvidence(requirementId, RequirementType.REQUIRED,
                "요구사항 " + requirementId, RetrievalStatus.COMPLETED, chunks);
    }

    private RetrievedEvidenceContextDto.RetrievedChunk chunk(Long chunkId, String content) {
        return new RetrievedEvidenceContextDto.RetrievedChunk(chunkId, 2L, 20L, UserDocumentType.RESUME,
                1, 1, 0, content.length(), 1, 0.9d, content);
    }
}
