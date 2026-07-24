package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.JobPostingAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPostingAnalysisRepository extends JpaRepository<JobPostingAnalysis, Long> {

    Optional<JobPostingAnalysis> findBySnapshot_SnapshotId(Long snapshotId);

    boolean existsBySnapshot_SnapshotId(Long snapshotId);

    void deleteBySnapshot_User_UserId(Long userId);
}
