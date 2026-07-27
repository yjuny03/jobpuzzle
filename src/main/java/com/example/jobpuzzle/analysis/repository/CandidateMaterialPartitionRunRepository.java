package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.CandidateMaterialPartitionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CandidateMaterialPartitionRunRepository extends JpaRepository<CandidateMaterialPartitionRun, Long> {
    Optional<CandidateMaterialPartitionRun> findBySnapshot_SnapshotIdAndRunFingerprint(Long snapshotId, String runFingerprint);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select run from CandidateMaterialPartitionRun run where run.partitionRunId = :runId")
    Optional<CandidateMaterialPartitionRun> findWithLockByPartitionRunId(@Param("runId") Long runId);
}
