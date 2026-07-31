package com.example.jobpuzzle.evaluation.repository;

import com.example.jobpuzzle.evaluation.entity.WeaknessRemediationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeaknessRemediationAttemptRepository
        extends JpaRepository<WeaknessRemediationAttempt, Long> {

    List<WeaknessRemediationAttempt> findByOriginTagLog_User_UserIdOrderByAttemptIdDesc(Long userId);

    List<WeaknessRemediationAttempt> findByOriginTagLog_TagLogIdOrderByAttemptIdAsc(Long tagLogId);

    Optional<WeaknessRemediationAttempt> findFirstByOriginTagLog_TagLogIdOrderByAttemptIdDesc(Long tagLogId);

    boolean existsByEvaluation_EvaluationId(Long evaluationId);
}
