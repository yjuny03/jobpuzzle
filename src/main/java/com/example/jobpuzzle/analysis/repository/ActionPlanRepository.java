package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.ActionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActionPlanRepository extends JpaRepository<ActionPlan, Long> {
    List<ActionPlan> findBySnapshot_SnapshotIdOrderByActionPlanIdAsc(Long snapshotId);
    Optional<ActionPlan> findBySnapshot_SnapshotIdAndTaskKey(Long snapshotId, String taskKey);
    boolean existsBySnapshot_SnapshotIdAndTaskKey(Long snapshotId, String taskKey);
    boolean existsBySnapshot_SnapshotId(Long snapshotId);
    void deleteBySnapshot_SnapshotId(Long snapshotId);
    void deleteBySnapshot_User_UserId(Long userId);
}
