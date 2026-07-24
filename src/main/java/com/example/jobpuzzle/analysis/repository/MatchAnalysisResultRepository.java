package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.MatchAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchAnalysisResultRepository extends JpaRepository<MatchAnalysisResult, Long> {
    List<MatchAnalysisResult> findBySnapshot_SnapshotIdOrderByMatchIdAsc(Long snapshotId);
    Optional<MatchAnalysisResult> findBySnapshot_SnapshotIdAndMatchKey(Long snapshotId, String matchKey);
    boolean existsBySnapshot_SnapshotIdAndMatchKey(Long snapshotId, String matchKey);
    boolean existsBySnapshot_SnapshotId(Long snapshotId);
    void deleteBySnapshot_SnapshotId(Long snapshotId);
    void deleteBySnapshot_User_UserId(Long userId);
}
