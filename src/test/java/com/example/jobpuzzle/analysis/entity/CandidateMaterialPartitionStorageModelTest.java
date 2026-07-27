package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CandidateMaterialPartitionStorageModelTest {

    @Test
    void keepsRunAndValidatedPartialPayloadLifecycleSeparateFromFinalJson02() {
        PromptTemplate prompt = mock(PromptTemplate.class);
        when(prompt.getVersion()).thenReturn("v1.1");
        CandidateMaterialPartitionRun run = CandidateMaterialPartitionRun.pending(mock(AnalysisInputSnapshot.class),
                "a".repeat(64), "b".repeat(64), AiProvider.ANTHROPIC, "claude-sonnet-5", prompt);
        CandidateMaterialPartition partition = CandidateMaterialPartition.pending(run, UserDocumentType.RESUME, 10L,
                0, 0, "seg-001", "seg-002", 2, "c".repeat(64), "d".repeat(64));

        run.start();
        partition.succeed("{\"availableDocumentTypes\":[\"RESUME\"]}");
        run.succeed();

        assertThat(run.getStatus()).isEqualTo(CandidateMaterialPartitionRunStatus.SUCCEEDED);
        assertThat(partition.getStatus()).isEqualTo(CandidateMaterialPartitionStatus.SUCCEEDED);
        assertThat(partition.getParsedResultJson()).contains("RESUME");
        assertThat(partition.getAiCallLog()).isNull();
    }
}
