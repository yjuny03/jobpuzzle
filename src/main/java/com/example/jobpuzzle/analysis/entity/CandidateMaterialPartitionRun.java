package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 하나의 snapshot·입력 manifest에 대한 JSON-02 partition batch다. 최종 CandidateMaterialAnalysis와는 별도다.
@Getter
@Entity
@NoArgsConstructor
@Table(name = "candidate_material_partition_run", uniqueConstraints = @UniqueConstraint(
        name = "uk_candidate_material_partition_run_snapshot_fingerprint",
        columnNames = {"snapshot_id", "run_fingerprint"}
), indexes = @Index(name = "idx_candidate_material_partition_run_snapshot_status", columnList = "snapshot_id,status"))
public class CandidateMaterialPartitionRun extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partition_run_id")
    private Long partitionRunId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "snapshot_id", nullable = false)
    private AnalysisInputSnapshot snapshot;

    // 전체 partition manifest와 provider/model/prompt/config 계약을 합친 SHA-256 값이다.
    @Column(name = "run_fingerprint", nullable = false, length = 64)
    private String runFingerprint;

    @Column(name = "manifest_hash", nullable = false, length = 64)
    private String manifestHash;

    @Enumerated(EnumType.STRING) @Column(name = "provider", nullable = false, length = 20)
    private AiProvider provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "prompt_template_id", nullable = false)
    private PromptTemplate promptTemplate;

    @Column(name = "prompt_version", nullable = false, length = 20)
    private String promptVersion;

    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20)
    private CandidateMaterialPartitionRunStatus status;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "batch_root_ai_call_log_id")
    private AiCallLog batchRootAiCallLog;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "final_analysis_id")
    private CandidateMaterialAnalysis finalAnalysis;

    public static CandidateMaterialPartitionRun pending(AnalysisInputSnapshot snapshot, String runFingerprint,
                                                         String manifestHash, AiProvider provider, String model,
                                                         PromptTemplate promptTemplate) {
        CandidateMaterialPartitionRun value = new CandidateMaterialPartitionRun();
        value.snapshot = snapshot; value.runFingerprint = runFingerprint; value.manifestHash = manifestHash;
        value.provider = provider; value.model = model; value.promptTemplate = promptTemplate;
        value.promptVersion = promptTemplate.getVersion(); value.status = CandidateMaterialPartitionRunStatus.PENDING;
        return value;
    }

    public void start() { this.status = CandidateMaterialPartitionRunStatus.RUNNING; }
    public void succeed() { this.status = CandidateMaterialPartitionRunStatus.SUCCEEDED; }
    public void fail() { this.status = CandidateMaterialPartitionRunStatus.FAILED; }
    public void finalizeWith(CandidateMaterialAnalysis analysis, AiCallLog rootLog) { this.finalAnalysis = analysis; this.batchRootAiCallLog = rootLog; this.status = CandidateMaterialPartitionRunStatus.SUCCEEDED; }
}
