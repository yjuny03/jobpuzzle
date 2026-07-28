package com.example.jobpuzzle.evaluation.repository;

import com.example.jobpuzzle.evaluation.entity.WeaknessTagLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeaknessTagLogRepository extends JpaRepository<WeaknessTagLog, Long> {
    List<WeaknessTagLog> findTop10ByUser_UserIdAndTagOrderByTagLogIdDesc(Long userId, String tag);
    List<WeaknessTagLog> findByUser_UserIdOrderByTagLogIdDesc(Long userId);
    boolean existsByEvaluation_EvaluationIdAndTag(Long evaluationId, String tag);
}
