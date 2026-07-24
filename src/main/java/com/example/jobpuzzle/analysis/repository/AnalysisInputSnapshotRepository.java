package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface AnalysisInputSnapshotRepository extends JpaRepository<AnalysisInputSnapshot, Long> {

    Optional<AnalysisInputSnapshot> findByAnalysisCase_AnalysisCaseIdAndUser_UserId(Long analysisCaseId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select snapshot from AnalysisInputSnapshot snapshot where snapshot.snapshotId = :snapshotId")
    Optional<AnalysisInputSnapshot> findWithLockBySnapshotId(@Param("snapshotId") Long snapshotId);

    void deleteByUser_UserId(Long userId);
}
