package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.CandidateMaterialPartition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CandidateMaterialPartitionRepository extends JpaRepository<CandidateMaterialPartition, Long> {
    List<CandidateMaterialPartition> findByPartitionRun_PartitionRunIdOrderByPartitionOrdinalAsc(Long partitionRunId);
    List<CandidateMaterialPartition> findByPartitionRun_PartitionRunIdAndStatusInOrderByPartitionOrdinalAsc(Long runId, java.util.Collection<com.example.jobpuzzle.analysis.entity.CandidateMaterialPartitionStatus> statuses);
    Optional<CandidateMaterialPartition> findFirstByInputFingerprintAndStatusOrderByPartitionIdDesc(String inputFingerprint, com.example.jobpuzzle.analysis.entity.CandidateMaterialPartitionStatus status);
    boolean existsByParentPartition_PartitionId(Long parentPartitionId);
}
