package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.config.AiGenerationProperties;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContext;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.analysis.entity.CandidateMaterialPartitionRunStatus;
import com.example.jobpuzzle.analysis.repository.CandidateMaterialAnalysisRepository;
import org.springframework.stereotype.Service;
import java.util.List;

// 기존 JSON-02 단일 호출을 대체하는 내부 경계다. 공용 AiClient와 JSON-01·05 경로는 건드리지 않는다.
@Service
public class CandidateMaterialPartitionOrchestrator {
    private final CandidateMaterialPartitionExecutor executor; private final CandidateMaterialPartitionFinalizer finalizer;
    private final CandidateMaterialAnalysisRepository analyses;
    private final AiGenerationProperties properties;
    public CandidateMaterialPartitionOrchestrator(CandidateMaterialPartitionExecutor executor, CandidateMaterialPartitionFinalizer finalizer,
                                                  CandidateMaterialAnalysisRepository analyses, AiGenerationProperties properties) { this.executor = executor; this.finalizer = finalizer; this.analyses = analyses; this.properties = properties; }
    public void execute(AnalysisInputSnapshotContext context, List<AnalysisInputSnapshotContextSource> sources) {
        if (analyses.existsBySnapshot_SnapshotId(context.getSnapshotId())) return;
        var limits = properties.getInputLimits();
        var run = executor.executeRun(context, sources, new CandidateMaterialPartitionPlanner.Limits(
                limits.getJson02PartitionMarkerContentChars(), limits.getJson02PartitionMaxMarkers()
        ));
        if (run.getStatus() != CandidateMaterialPartitionRunStatus.SUCCEEDED) throw new AiProcessingException(AiCallLogErrorType.PROVIDER_ERROR, "candidate material partition execution failed");
        finalizer.finalizeRun(run.getPartitionRunId(), sources);
    }
}
