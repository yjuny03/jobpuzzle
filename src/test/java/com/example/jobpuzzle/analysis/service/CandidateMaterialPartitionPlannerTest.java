package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.validation.AnalysisSourceMarkerParser;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CandidateMaterialPartitionPlannerTest {

    private final CandidateMaterialPartitionPlanner planner = new CandidateMaterialPartitionPlanner(new AnalysisSourceMarkerParser());

    @Test
    void keepsExtractionAndMarkerOrderWhenOnePartitionIsEnough() {
        var result = planner.plan(List.of(source(UserDocumentType.RESUME, 10L, 20L, "first", "second")), limits(100, 8));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().markers()).extracting(AnalysisSourceMarkerParser.SourceMarker::segmentId)
                .containsExactly("seg-001", "seg-002");
        assertThat(result.getFirst().markerContentHash()).hasSize(64);
    }

    @Test
    void splitsOnlyBetweenMarkersAndPreservesGlobalSourceOrder() {
        var result = planner.plan(List.of(
                source(UserDocumentType.RESUME, 10L, 20L, "aaaa", "bbbb", "cccc"),
                source(UserDocumentType.PORTFOLIO, 11L, 21L, "dddd")
        ), limits(8, 2));

        assertThat(result).hasSize(3);
        assertThat(result).extracting(CandidateMaterialMarkerPartition::partitionOrdinal).containsExactly(0, 1, 2);
        assertThat(result.get(0).markers()).extracting(AnalysisSourceMarkerParser.SourceMarker::segmentId).containsExactly("seg-001", "seg-002");
        assertThat(result.get(1).markers()).extracting(AnalysisSourceMarkerParser.SourceMarker::segmentId).containsExactly("seg-003");
        assertThat(result.get(2).documentType()).isEqualTo(UserDocumentType.PORTFOLIO);
    }

    @Test
    void keepsOversizedMarkerAsOneExplicitPartitionInsteadOfTruncatingIt() {
        var result = planner.plan(List.of(source(UserDocumentType.RESUME, 10L, 20L, "x".repeat(11))), limits(10, 8));

        assertThat(result).singleElement().satisfies(partition -> {
            assertThat(partition.oversizedSingleMarker()).isTrue();
            assertThat(partition.markerContentChars()).isEqualTo(11);
            assertThat(partition.markers()).hasSize(1);
        });
    }

    @Test
    void rejectsInvalidLimitsWithoutSilentlyBuildingAnEmptyPlan() {
        assertThatThrownBy(() -> planner.plan(List.of(), limits(0, 1))).isInstanceOf(IllegalArgumentException.class);
        assertThat(planner.plan(List.of(), limits(10, 1))).isEmpty();
    }

    private CandidateMaterialPartitionPlanner.Limits limits(int chars, int markers) {
        return new CandidateMaterialPartitionPlanner.Limits(chars, markers);
    }

    private AnalysisInputSnapshotContextSource source(UserDocumentType type, Long extractionId, Long documentId, String... bodies) {
        StringBuilder analysisText = new StringBuilder();
        for (int index = 0; index < bodies.length; index++) {
            analysisText.append("[SOURCE extractionId=").append(extractionId).append(" documentId=").append(documentId)
                    .append("][PAGE=").append(index + 1).append("][SEGMENT=seg-").append(String.format("%03d", index + 1)).append("]\n")
                    .append(bodies[index]).append("\n");
        }
        AnalysisInputSnapshotContextSource source = mock(AnalysisInputSnapshotContextSource.class);
        when(source.getDocumentType()).thenReturn(type); when(source.getExtractionId()).thenReturn(extractionId);
        when(source.getDocumentId()).thenReturn(documentId); when(source.getAnalysisText()).thenReturn(analysisText.toString());
        return source;
    }
}
