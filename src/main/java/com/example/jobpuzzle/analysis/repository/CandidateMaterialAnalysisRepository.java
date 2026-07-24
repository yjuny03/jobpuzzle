package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.CandidateMaterialAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateMaterialAnalysisRepository extends JpaRepository<CandidateMaterialAnalysis, Long> {

    Optional<CandidateMaterialAnalysis> findBySnapshot_SnapshotId(Long snapshotId);

    boolean existsBySnapshot_SnapshotId(Long snapshotId);

    void deleteBySnapshot_User_UserId(Long userId);
}
