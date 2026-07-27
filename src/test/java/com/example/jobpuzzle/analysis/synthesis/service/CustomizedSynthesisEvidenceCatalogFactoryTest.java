package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.rag.dto.RetrievedEvidenceContextDto;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalStatus;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomizedSynthesisEvidenceCatalogFactoryTest {

    private final CustomizedSynthesisEvidenceCatalogFactory factory =
            new CustomizedSynthesisEvidenceCatalogFactory();

    @Test
    void createsDeterministicCatalogAndDeduplicatesSharedPostingAndCandidateEvidence() {
        SourceReference sharedPosting = SourceReference.builder()
                .extractionId(1L).documentId(10L).documentType(UserDocumentType.JOB_POSTING)
                .pageNumber(2).segmentId("posting-segment").evidenceText("공통 공고 근거").build();
        JobPostingAnalysisResult posting = posting(
                requirement("req-1", "필수 조건", sharedPosting),
                requirement("req-2", "우대 조건", sharedPosting));
        RetrievedEvidenceContextDto.RetrievedChunk sharedCandidate = chunk(101L, "공통 후보자 근거");
        RetrievedEvidenceContextDto retrieval = new RetrievedEvidenceContextDto(List.of(
                retrieved("req-1", RequirementType.REQUIRED, "필수 조건", List.of(sharedCandidate)),
                retrieved("req-2", RequirementType.PREFERRED, "우대 조건", List.of(sharedCandidate))));

        CustomizedSynthesisEvidenceCatalog catalog = factory.create(posting, retrieval);

        assertThat(catalog.requirements()).extracting(CustomizedSynthesisEvidenceCatalog.RequirementItem::requirementId)
                .containsExactly("req-1", "req-2");
        assertThat(catalog.requirements()).allSatisfy(requirement -> {
            assertThat(requirement.postingEvidenceIds()).containsExactly("posting-1");
            assertThat(requirement.allowedCandidateEvidenceIds()).containsExactly("candidate-chunk-101");
        });
        assertThat(catalog.evidence()).hasSize(2);
        assertThat(catalog.evidence()).extracting(CustomizedSynthesisEvidenceCatalog.EvidenceItem::evidenceId)
                .containsExactly("posting-1", "candidate-chunk-101");
        assertThat(catalog.evidence().get(0).sourceReference()).satisfies(source -> {
            assertThat(source.getExtractionId()).isEqualTo(1L);
            assertThat(source.getDocumentId()).isEqualTo(10L);
            assertThat(source.getDocumentType()).isEqualTo(UserDocumentType.JOB_POSTING);
            assertThat(source.getPageNumber()).isEqualTo(2);
            assertThat(source.getSegmentId()).isEqualTo("posting-segment");
            assertThat(source.getEvidenceText()).isEqualTo("공통 공고 근거");
        });
        assertThat(catalog.evidence().get(1).sourceReference()).satisfies(source -> {
            assertThat(source.getExtractionId()).isEqualTo(2L);
            assertThat(source.getDocumentId()).isEqualTo(20L);
            assertThat(source.getDocumentType()).isEqualTo(UserDocumentType.RESUME);
            assertThat(source.getEvidenceText()).isEqualTo("공통 후보자 근거");
        });
    }

    @Test
    void rejectsRetrievalRequirementSetThatDiffersFromJson01() {
        JobPostingAnalysisResult posting = posting(
                requirement("req-1", "필수 조건", postingSource()),
                requirement("req-2", "우대 조건", postingSource()));
        RetrievedEvidenceContextDto retrieval = new RetrievedEvidenceContextDto(List.of(
                retrieved("req-1", RequirementType.REQUIRED, "필수 조건", List.of(chunk(101L, "후보자 근거")))));

        assertThatThrownBy(() -> factory.create(posting, retrieval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JSON-01 and retrieval requirement IDs differ");
    }

    @Test
    void rejectsRetrievalMetadataThatDiffersFromJson01Authority() {
        JobPostingAnalysisResult posting = posting(
                requirement("req-1", "필수 조건", postingSource()), null);
        RetrievedEvidenceContextDto retrieval = new RetrievedEvidenceContextDto(List.of(
                retrieved("req-1", RequirementType.PREFERRED, "변경된 조건", List.of(chunk(101L, "후보자 근거")))));

        assertThatThrownBy(() -> factory.create(posting, retrieval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retrieval requirement metadata differs from JSON-01");
    }

    private JobPostingAnalysisResult posting(JobPostingAnalysisResult.Requirement required,
                                             JobPostingAnalysisResult.Requirement preferred) {
        return JobPostingAnalysisResult.builder()
                .mainTasks(List.of())
                .requirements(required == null ? List.of() : List.of(required))
                .preferred(preferred == null ? List.of() : List.of(preferred))
                .companyValues(List.of()).coreCompetencies(List.of()).conflicts(List.of()).missingEvidence(List.of())
                .build();
    }

    private JobPostingAnalysisResult.Requirement requirement(String id, String text, SourceReference source) {
        return JobPostingAnalysisResult.Requirement.builder()
                .requirementId(id).text(text).sourceRefs(List.of(source)).build();
    }

    private RetrievedEvidenceContextDto.RequirementEvidence retrieved(
            String id, RequirementType type, String text, List<RetrievedEvidenceContextDto.RetrievedChunk> chunks) {
        return new RetrievedEvidenceContextDto.RequirementEvidence(id, type, text, RetrievalStatus.COMPLETED, chunks);
    }

    private RetrievedEvidenceContextDto.RetrievedChunk chunk(Long id, String content) {
        return new RetrievedEvidenceContextDto.RetrievedChunk(
                id, 2L, 20L, UserDocumentType.RESUME, 3, 3, 0, content.length(), 1, 0.9d, content);
    }

    private SourceReference postingSource() {
        return SourceReference.builder()
                .extractionId(1L).documentId(10L).documentType(UserDocumentType.JOB_POSTING)
                .pageNumber(1).segmentId("posting-segment").evidenceText("공고 근거").build();
    }
}
