package com.example.jobpuzzle.ai.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Optional<AiCallLog> findFirstByExecutionStageAndInputReferenceTypeAndInputReferenceIdOrderByAiCallLogIdDesc(
            AiExecutionStage executionStage, AiInputReferenceType inputReferenceType, String inputReferenceId);

    Optional<AiCallLog> findFirstByInputReferenceTypeAndInputReferenceIdOrderByAiCallLogIdDesc(
            AiInputReferenceType inputReferenceType, String inputReferenceId);

    // 관리자 AI 오류 로그 조회 - status·stage가 없으면 전체
    @Query("SELECT l FROM AiCallLog l WHERE (:status IS NULL OR l.status = :status) "
            + "AND (:stage IS NULL OR l.executionStage = :stage)")
    Page<AiCallLog> search(@Param("status") AiCallLogStatus status, @Param("stage") AiExecutionStage stage, Pageable pageable);
}
