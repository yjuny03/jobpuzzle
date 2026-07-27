package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.ai.validation.AiResponseProcessor;
import com.example.jobpuzzle.ai.validation.AnalysisSourceMarkerParser;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.analysis.entity.CandidateMaterialPartition;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CandidateMaterialPartitionMergerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CandidateMaterialPartitionMerger merger = new CandidateMaterialPartitionMerger(mapper, new AiResponseProcessor(mapper, new AnalysisSourceMarkerParser()));

    @Test
    void preservesSimilarFactsWhenTheirSourceMarkersDifferAndRevalidatesEvidence() throws Exception {
        CandidateMaterialPartition first = partition(0, payload("seg-001"));
        CandidateMaterialPartition second = partition(1, payload("seg-002"));
        AnalysisInputSnapshotContextSource source = mock(AnalysisInputSnapshotContextSource.class);
        when(source.getDocumentType()).thenReturn(UserDocumentType.RESUME);
        when(source.getAnalysisText()).thenReturn("[SOURCE extractionId=10 documentId=20][PAGE=1][SEGMENT=seg-001]\nfirst evidence\n[SOURCE extractionId=10 documentId=20][PAGE=2][SEGMENT=seg-002]\nsecond evidence");

        var merged = merger.merge(List.of(second, first), List.of(source));

        assertThat(merged.getResume().getExperiences()).hasSize(2);
        assertThat(merged.getResume().getExperiences()).extracting(CandidateMaterialAnalysisResult.Experience::getExperienceId)
                .containsExactly("exp-1", "exp-2");
        assertThat(merged.getResume().getExperiences()).extracting(value -> value.getSourceRefs().getFirst().getEvidenceText())
                .containsExactly("first evidence", "second evidence");
    }

    private CandidateMaterialPartition partition(int ordinal, String payload) {
        CandidateMaterialPartition partition = mock(CandidateMaterialPartition.class);
        when(partition.getPartitionOrdinal()).thenReturn(ordinal);
        when(partition.getParsedResultJson()).thenReturn(payload);
        return partition;
    }

    private String payload(String segmentId) throws Exception {
        SourceReference ref = SourceReference.builder().extractionId(10L).documentId(20L).documentType(UserDocumentType.RESUME)
                .pageNumber(segmentId.endsWith("001") ? 1 : 2).segmentId(segmentId).build();
        var experience = CandidateMaterialAnalysisResult.Experience.builder().experienceId("model-id").title("backend")
                .period(null).summary("API development").sourceRefs(List.of(ref)).build();
        return mapper.writeValueAsString(CandidateMaterialAnalysisResult.builder().availableDocumentTypes(List.of(UserDocumentType.RESUME))
                .resume(CandidateMaterialAnalysisResult.Resume.builder().experiences(List.of(experience)).skills(List.of()).roles(List.of()).results(List.of()).build())
                .coverLetter(null).portfolio(null).experienceNote(null).missingEvidence(List.of()).build());
    }
}
