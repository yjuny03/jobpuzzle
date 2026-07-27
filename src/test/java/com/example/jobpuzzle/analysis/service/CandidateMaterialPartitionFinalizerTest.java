package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.log.*;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.analysis.entity.*;
import com.example.jobpuzzle.analysis.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CandidateMaterialPartitionFinalizerTest {
    @Test
    void createsAggregateRootOnlyAfterAllPartitionsSucceeded() {
        CandidateMaterialPartitionRunRepository runs = mock(CandidateMaterialPartitionRunRepository.class);
        CandidateMaterialPartitionRepository partitions = mock(CandidateMaterialPartitionRepository.class);
        CandidateMaterialAnalysisRepository analyses = mock(CandidateMaterialAnalysisRepository.class);
        AiCallLogRepository logs = mock(AiCallLogRepository.class);
        CandidateMaterialPartitionMerger merger = mock(CandidateMaterialPartitionMerger.class);
        AnalysisInputSnapshot snapshot = mock(AnalysisInputSnapshot.class); when(snapshot.getSnapshotId()).thenReturn(7L);
        PromptTemplate prompt = PromptTemplate.builder().promptCode("PT").name("json02").version("v1.1").targetJson("JSON-02").templateText("x").isActive(true).build();
        CandidateMaterialPartitionRun run = CandidateMaterialPartitionRun.pending(snapshot, "a".repeat(64), "b".repeat(64), AiProvider.MOCK, "mock", prompt);
        AiCallLog call = AiCallLog.pending(AiProvider.MOCK, "mock", AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS,
                AiInputReferenceType.ANALYSIS_SNAPSHOT, "7", "c".repeat(64), prompt, null, null); call.start(); call.succeed();
        CandidateMaterialPartition partition = mock(CandidateMaterialPartition.class);
        when(partition.getStatus()).thenReturn(CandidateMaterialPartitionStatus.SUCCEEDED); when(partition.getAiCallLog()).thenReturn(call);
        when(runs.findWithLockByPartitionRunId(1L)).thenReturn(Optional.of(run)); when(analyses.existsBySnapshot_SnapshotId(7L)).thenReturn(false);
        when(partitions.findByPartitionRun_PartitionRunIdOrderByPartitionOrdinalAsc(1L)).thenReturn(List.of(partition));
        when(merger.merge(any(), any())).thenReturn(CandidateMaterialAnalysisResult.builder().availableDocumentTypes(List.of()).missingEvidence(List.of()).build());
        when(analyses.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new CandidateMaterialPartitionFinalizer(runs, partitions, analyses, logs, merger).finalizeRun(1L, List.of());

        ArgumentCaptor<AiCallLog> root = ArgumentCaptor.forClass(AiCallLog.class);
        verify(logs).saveAndFlush(root.capture());
        assertThat(root.getValue().getCallRole()).isEqualTo(AiCallLogRole.AGGREGATE_RESULT);
        assertThat(run.getBatchRootAiCallLog()).isSameAs(root.getValue());
        assertThat(run.getFinalAnalysis()).isNotNull();
    }
}
