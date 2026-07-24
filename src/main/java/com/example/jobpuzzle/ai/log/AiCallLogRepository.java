package com.example.jobpuzzle.ai.log;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {

    Optional<AiCallLog> findFirstByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndInputFingerprintOrderByAiCallLogIdDesc(
            AiExecutionStage executionStage,
            AiInputReferenceType inputReferenceType,
            String inputReferenceId,
            String inputFingerprint
    );

    List<AiCallLog> findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatusIn(
            AiExecutionStage executionStage,
            AiInputReferenceType inputReferenceType,
            String inputReferenceId,
            Collection<AiCallLogStatus> statuses
    );

    List<AiCallLog> findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatus(
            AiExecutionStage executionStage,
            AiInputReferenceType inputReferenceType,
            String inputReferenceId,
            AiCallLogStatus status
    );
    Optional<AiCallLog> findFirstByInputReferenceTypeAndInputReferenceIdAndStatusOrderByAiCallLogIdDesc(
            AiInputReferenceType inputReferenceType, String inputReferenceId, AiCallLogStatus status);
}
