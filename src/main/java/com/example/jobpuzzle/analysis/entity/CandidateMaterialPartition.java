package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiFailureKind;
import com.example.jobpuzzle.document.entity.UserDocumentType;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

// marker를 자르지 않은 단일 JSON-02 호출 단위와 검증 완료된 partial JSON payload를 보관한다.
@Getter
@Entity
@NoArgsConstructor
@Table(name = "candidate_material_partition", uniqueConstraints = @UniqueConstraint(
        name = "uk_candidate_material_partition_run_ordinal", columnNames = {"partition_run_id", "partition_ordinal"}
), indexes = @Index(name = "idx_candidate_material_partition_run_status", columnList = "partition_run_id,status"))
public class CandidateMaterialPartition extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partition_id")
    private Long partitionId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "partition_run_id", nullable = false)
    private CandidateMaterialPartitionRun partitionRun;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_partition_id")
    private CandidateMaterialPartition parentPartition;

    @Column(name = "partition_depth", nullable = false)
    private int partitionDepth;

    @Enumerated(EnumType.STRING) @Column(name = "document_type", nullable = false, length = 30)
    private UserDocumentType documentType;

    @Column(name = "extraction_id", nullable = false)
    private Long extractionId;

    @Column(name = "partition_index", nullable = false)
    private int partitionIndex;

    // snapshot 전체 원문 순서를 보존하는 ordinal이며, 병합 정렬의 유일한 기준이다.
    @Column(name = "partition_ordinal", nullable = false)
    private int partitionOrdinal;

    @Column(name = "first_segment_id", nullable = false, length = 100)
    private String firstSegmentId;

    @Column(name = "last_segment_id", nullable = false, length = 100)
    private String lastSegmentId;

    @Column(name = "marker_count", nullable = false)
    private int markerCount;

    @Column(name = "marker_content_hash", nullable = false, length = 64)
    private String markerContentHash;

    @Column(name = "input_fingerprint", nullable = false, length = 64)
    private String inputFingerprint;

    @Enumerated(EnumType.STRING) @Column(name = "failure_kind", length = 40)
    private AiFailureKind failureKind;

    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20)
    private CandidateMaterialPartitionStatus status;

    // raw Provider 원문이 아닌 parser/validator 통과 뒤의 partial DTO JSON만 저장한다.
    @Lob @Column(name = "parsed_result_json", columnDefinition = "longtext")
    private String parsedResultJson;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ai_call_log_id")
    private AiCallLog aiCallLog;

    public static CandidateMaterialPartition pending(CandidateMaterialPartitionRun run, UserDocumentType documentType,
                                                     Long extractionId, int partitionIndex, int partitionOrdinal,
                                                     String firstSegmentId, String lastSegmentId, int markerCount,
                                                     String markerContentHash, String inputFingerprint) {
        CandidateMaterialPartition value = new CandidateMaterialPartition();
        value.partitionRun = run; value.documentType = documentType; value.extractionId = extractionId;
        value.partitionDepth = 0; value.failureKind = AiFailureKind.NONE;
        value.partitionIndex = partitionIndex; value.partitionOrdinal = partitionOrdinal;
        value.firstSegmentId = firstSegmentId; value.lastSegmentId = lastSegmentId; value.markerCount = markerCount;
        value.markerContentHash = markerContentHash; value.inputFingerprint = inputFingerprint;
        value.status = CandidateMaterialPartitionStatus.PENDING;
        return value;
    }

    public void start(AiCallLog log) { this.aiCallLog = log; this.status = CandidateMaterialPartitionStatus.RUNNING; }
    public void succeed(String parsedResultJson) { this.parsedResultJson = parsedResultJson; this.status = CandidateMaterialPartitionStatus.SUCCEEDED; }
    public void fail(AiFailureKind kind) { this.failureKind = kind; this.status = CandidateMaterialPartitionStatus.FAILED; }
    public void split() { this.status = CandidateMaterialPartitionStatus.SPLIT; }
    public void reuseFrom(CandidateMaterialPartition source) {
        this.parsedResultJson = source.parsedResultJson; this.aiCallLog = source.aiCallLog;
        this.failureKind = AiFailureKind.NONE; this.status = CandidateMaterialPartitionStatus.SUCCEEDED;
    }
    public static CandidateMaterialPartition child(CandidateMaterialPartition parent, int childOrdinal,
                                                    String firstSegmentId, String lastSegmentId, int markerCount,
                                                    String markerContentHash, String inputFingerprint) {
        CandidateMaterialPartition value = pending(parent.partitionRun, parent.documentType, parent.extractionId,
                parent.partitionIndex, childOrdinal, firstSegmentId, lastSegmentId, markerCount, markerContentHash, inputFingerprint);
        value.parentPartition = parent; value.partitionDepth = parent.partitionDepth + 1;
        return value;
    }
}
