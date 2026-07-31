package com.example.jobpuzzle.admin.dto;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.log.AiCallLogRole;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiInputReferenceType;
import com.example.jobpuzzle.ai.log.AiProvider;
import lombok.Getter;

import java.time.LocalDateTime;

// AI 분석 오류 로그 단건 조회 - 목록에는 없는 프롬프트·가이드 계보, 재시도 이력까지 노출
@Getter
public class AdminAiCallLogDetailResponse {

    private final Long aiCallLogId;
    private final AiCallLogRole callRole;
    private final AiProvider provider;
    private final String model;
    private final AiExecutionStage executionStage;
    private final AiInputReferenceType inputReferenceType;
    private final String inputReferenceId;
    private final String inputFingerprint;
    private final Long promptTemplateId;
    private final String promptCode;
    private final String promptName;
    private final String promptVersion;
    private final Long guideId;
    private final String guideCode;
    private final String guideTitle;
    private final String guideVersion;
    private final AiCallLogStatus status;
    private final Boolean valid;
    private final AiCallLogErrorType errorType;
    private final String errorMessage;
    private final String providerCompletionMetadata;
    private final boolean reused;
    private final Long reusedFromCallId;
    private final Long parentAiCallLogId;
    private final int retryCount;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;

    private AdminAiCallLogDetailResponse(
            Long aiCallLogId, AiCallLogRole callRole, AiProvider provider, String model,
            AiExecutionStage executionStage, AiInputReferenceType inputReferenceType, String inputReferenceId,
            String inputFingerprint, Long promptTemplateId, String promptCode, String promptName, String promptVersion,
            Long guideId, String guideCode, String guideTitle, String guideVersion,
            AiCallLogStatus status, Boolean valid, AiCallLogErrorType errorType, String errorMessage,
            String providerCompletionMetadata, boolean reused, Long reusedFromCallId, Long parentAiCallLogId,
            int retryCount, LocalDateTime startedAt, LocalDateTime completedAt
    ) {
        this.aiCallLogId = aiCallLogId;
        this.callRole = callRole;
        this.provider = provider;
        this.model = model;
        this.executionStage = executionStage;
        this.inputReferenceType = inputReferenceType;
        this.inputReferenceId = inputReferenceId;
        this.inputFingerprint = inputFingerprint;
        this.promptTemplateId = promptTemplateId;
        this.promptCode = promptCode;
        this.promptName = promptName;
        this.promptVersion = promptVersion;
        this.guideId = guideId;
        this.guideCode = guideCode;
        this.guideTitle = guideTitle;
        this.guideVersion = guideVersion;
        this.status = status;
        this.valid = valid;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.providerCompletionMetadata = providerCompletionMetadata;
        this.reused = reused;
        this.reusedFromCallId = reusedFromCallId;
        this.parentAiCallLogId = parentAiCallLogId;
        this.retryCount = retryCount;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public static AdminAiCallLogDetailResponse from(AiCallLog log) {
        var promptTemplate = log.getPromptTemplate();
        var guide = log.getGuide();
        var reusedFromCall = log.getReusedFromCall();
        var parentAiCallLog = log.getParentAiCallLog();
        return new AdminAiCallLogDetailResponse(
                log.getAiCallLogId(),
                log.getCallRole(),
                log.getProvider(),
                log.getModel(),
                log.getExecutionStage(),
                log.getInputReferenceType(),
                log.getInputReferenceId(),
                log.getInputFingerprint(),
                promptTemplate == null ? null : promptTemplate.getPromptTemplateId(),
                promptTemplate == null ? null : promptTemplate.getPromptCode(),
                promptTemplate == null ? null : promptTemplate.getName(),
                log.getPromptVersion(),
                guide == null ? null : guide.getGuideId(),
                guide == null ? null : guide.getGuideCode(),
                guide == null ? null : guide.getTitle(),
                log.getGuideVersion(),
                log.getStatus(),
                log.getValid(),
                log.getErrorType(),
                log.getErrorMessage(),
                log.getProviderCompletionMetadata(),
                log.isReused(),
                reusedFromCall == null ? null : reusedFromCall.getAiCallLogId(),
                parentAiCallLog == null ? null : parentAiCallLog.getAiCallLogId(),
                log.getRetryCount(),
                log.getStartedAt(),
                log.getCompletedAt()
        );
    }
}
