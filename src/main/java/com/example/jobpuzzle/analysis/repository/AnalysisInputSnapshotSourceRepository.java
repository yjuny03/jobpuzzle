package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshotSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisInputSnapshotSourceRepository extends JpaRepository<AnalysisInputSnapshotSource, Long> {

    List<AnalysisInputSnapshotSource> findBySnapshot_SnapshotIdOrderBySnapshotSourceIdAsc(Long snapshotId);

    void deleteBySnapshot_User_UserId(Long userId);
}