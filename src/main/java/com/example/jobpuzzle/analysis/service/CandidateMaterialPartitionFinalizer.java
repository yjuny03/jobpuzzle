package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.log.*;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.analysis.entity.*;
import com.example.jobpuzzle.analysis.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;

// 모든 partition 성공 뒤에만 대표 root log와 최종 JSON-02를 같은 트랜잭션에서 확정한다.
@Service
public class CandidateMaterialPartitionFinalizer {
    private final CandidateMaterialPartitionRunRepository runs; private final CandidateMaterialPartitionRepository partitions;
    private final CandidateMaterialAnalysisRepository analyses; private final AiCallLogRepository logs; private final CandidateMaterialPartitionMerger merger;
    public CandidateMaterialPartitionFinalizer(CandidateMaterialPartitionRunRepository runs, CandidateMaterialPartitionRepository partitions,
                                               CandidateMaterialAnalysisRepository analyses, AiCallLogRepository logs, CandidateMaterialPartitionMerger merger) {
        this.runs = runs; this.partitions = partitions; this.analyses = analyses; this.logs = logs; this.merger = merger;
    }
    @Transactional
    public CandidateMaterialAnalysis finalizeRun(Long runId, List<AnalysisInputSnapshotContextSource> sources) {
        CandidateMaterialPartitionRun run = runs.findWithLockByPartitionRunId(runId).orElseThrow();
        if (run.getFinalAnalysis() != null) return run.getFinalAnalysis();
        if (analyses.existsBySnapshot_SnapshotId(run.getSnapshot().getSnapshotId())) throw new IllegalStateException("final JSON-02 already exists");
        List<CandidateMaterialPartition> values = partitions.findByPartitionRun_PartitionRunIdOrderByPartitionOrdinalAsc(runId);
        if (values.isEmpty() || values.stream().anyMatch(value -> value.getStatus() != CandidateMaterialPartitionStatus.SUCCEEDED
                || value.getAiCallLog() == null || value.getAiCallLog().getStatus() != AiCallLogStatus.SUCCEEDED
                || value.getAiCallLog().getProvider() != run.getProvider() || !run.getModel().equals(value.getAiCallLog().getModel()))) {
            throw new IllegalStateException("all partitions must succeed with the run provider/model");
        }
        var result = merger.merge(values, sources);
        AiCallLog root = AiCallLog.aggregateResult(run.getProvider(), run.getModel(), AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS,
                AiInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(run.getSnapshot().getSnapshotId()), run.getRunFingerprint(), run.getPromptTemplate());
        root.start(); logs.saveAndFlush(root);
        CandidateMaterialAnalysis analysis = analyses.saveAndFlush(CandidateMaterialAnalysis.from(run.getSnapshot(), result, root));
        root.succeed(); run.finalizeWith(analysis, root);
        return analysis;
    }
}
