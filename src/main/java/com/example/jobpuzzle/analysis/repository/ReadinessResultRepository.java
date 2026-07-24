package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.ReadinessResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReadinessResultRepository extends JpaRepository<ReadinessResult, Long> {
    Optional<ReadinessResult> findBySnapshot_SnapshotId(Long snapshotId);
    boolean existsBySnapshot_SnapshotId(Long snapshotId);
    void deleteBySnapshot_SnapshotId(Long snapshotId);
    void deleteBySnapshot_User_UserId(Long userId);
}
