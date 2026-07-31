package com.example.jobpuzzle.evaluation.repository;

import com.example.jobpuzzle.evaluation.entity.WeaknessTagLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeaknessTagLogRepository extends JpaRepository<WeaknessTagLog, Long> {
    List<WeaknessTagLog> findTop10ByUser_UserIdAndTagOrderByTagLogIdDesc(Long userId, String tag);
    List<WeaknessTagLog> findByUser_UserIdOrderByTagLogIdDesc(Long userId);
    boolean existsByEvaluation_EvaluationIdAndTag(Long evaluationId, String tag);
    Optional<WeaknessTagLog> findByEvaluation_EvaluationIdAndTag(Long evaluationId, String tag);
}
