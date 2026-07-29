package com.example.jobpuzzle.admin.dto;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiInputReferenceType;
import com.example.jobpuzzle.ai.log.AiProvider;
import lombok.Getter;

import java.time.LocalDateTime;

// AI 분석 오류 로그 조회 - 운영 추적용, 원본 로그를 그대로 노출
@Getter
public class AdminAiCallLogResponse {

    private final Long aiCallLogId;
    private final AiProvider provider;
    private final String model;
    private final AiExecutionStage executionStage;
    private final AiInputReferenceType inputReferenceType;
    private final String inputReferenceId;
    private final AiCallLogStatus status;
    private final AiCallLogErrorType errorType;
    private final String errorMessage;
    private final int retryCount;
    private final boolean reused;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;

    private AdminAiCallLogResponse(
            Long aiCallLogId, AiProvider provider, String model, AiExecutionStage executionStage,
            AiInputReferenceType inputReferenceType, String inputReferenceId,
            AiCallLogStatus status, AiCallLogErrorType errorType, String errorMessage,
            int retryCount, boolean reused, LocalDateTime startedAt, LocalDateTime completedAt
    ) {
        this.aiCallLogId = aiCallLogId;
        this.provider = provider;
        this.model = model;
        this.executionStage = executionStage;
        this.inputReferenceType = inputReferenceType;
        this.inputReferenceId = inputReferenceId;
        this.status = status;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.reused = reused;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public static AdminAiCallLogResponse from(AiCallLog log) {
        return new AdminAiCallLogResponse(
                log.getAiCallLogId(),
                log.getProvider(),
                log.getModel(),
                log.getExecutionStage(),
                log.getInputReferenceType(),
                log.getInputReferenceId(),
                log.getStatus(),
                log.getErrorType(),
                log.getErrorMessage(),
                log.getRetryCount(),
                log.isReused(),
                log.getStartedAt(),
                log.getCompletedAt()
        );
    }
}