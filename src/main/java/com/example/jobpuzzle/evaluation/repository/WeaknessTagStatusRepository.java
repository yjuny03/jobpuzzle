package com.example.jobpuzzle.evaluation.repository;

import com.example.jobpuzzle.evaluation.entity.WeaknessTagStatus;
import com.example.jobpuzzle.evaluation.entity.WeaknessTagResolveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeaknessTagStatusRepository extends JpaRepository<WeaknessTagStatus, Long> {
    Optional<WeaknessTagStatus> findByUser_UserIdAndTag(Long userId, String tag);
    List<WeaknessTagStatus> findByUser_UserIdAndStatus(Long userId, WeaknessTagResolveStatus status);
    boolean existsByUser_UserIdAndStatus(Long userId, WeaknessTagResolveStatus status);
}
