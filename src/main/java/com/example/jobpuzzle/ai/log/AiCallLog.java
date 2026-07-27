package com.example.jobpuzzle.ai.log;

import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// AI 호출의 입력 기준과 실행 상태를 남겨 단계별 재사용·재시도를 판단한다.
@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "ai_call_log",
        indexes = @Index(
                name = "idx_ai_call_log_stage_fingerprint_status",
                columnList = "execution_stage,input_fingerprint,status"
        )
)
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_call_log_id")
    private Long aiCallLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AiProvider provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    // 기존 DB 행은 null일 수 있으므로 비용 집계에서는 null을 PROVIDER_CALL로 해석한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "call_role", length = 30)
    private AiCallLogRole callRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_stage", nullable = false, length = 50)
    private AiExecutionStage executionStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_reference_type", nullable = false, length = 30)
    private AiInputReferenceType inputReferenceType;

    @Column(name = "input_reference_id", nullable = false, length = 100)
    private String inputReferenceId;

    @Column(name = "input_fingerprint", nullable = false, length = 64)
    private String inputFingerprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id", nullable = false)
    private PromptTemplate promptTemplate;

    @Column(name = "prompt_version", nullable = false, length = 20)
    private String promptVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private JobGuideDocument guide;

    @Column(name = "guide_version", length = 20)
    private String guideVersion;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiCallLogStatus status;

    @Column(name = "valid")
    private Boolean valid;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 30)
    private AiCallLogErrorType errorType;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    // 원문·비밀값 없이 provider 종료/usage와 서버 후처리 결과만 JSON으로 보관한다.
    @jakarta.persistence.Lob
    @Column(name = "provider_completion_metadata", columnDefinition = "longtext")
    private String providerCompletionMetadata;

    @Column(name = "reused", nullable = false)
    private boolean reused;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reused_from_call_id")
    private AiCallLog reusedFromCall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_ai_call_log_id")
    private AiCallLog parentAiCallLog;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    public static AiCallLog pending(
            AiProvider provider,
            String model,
            AiExecutionStage executionStage,
            AiInputReferenceType inputReferenceType,
            String inputReferenceId,
            String inputFingerprint,
            PromptTemplate promptTemplate,
            JobGuideDocument guide,
            AiCallLog parentAiCallLog
    ) {
        AiCallLog log = new AiCallLog();
        log.provider = provider;
        log.model = model;
        log.callRole = AiCallLogRole.PROVIDER_CALL;
        log.executionStage = executionStage;
        log.inputReferenceType = inputReferenceType;
        log.inputReferenceId = inputReferenceId;
        log.inputFingerprint = inputFingerprint;
        log.promptTemplate = promptTemplate;
        log.promptVersion = promptTemplate.getVersion();
        log.guide = guide;
        log.guideVersion = guide == null ? null : guide.getVersion();
        log.status = AiCallLogStatus.PENDING;
        log.errorType = AiCallLogErrorType.NONE;
        log.reused = false;
        log.retryCount = parentAiCallLog == null ? 0 : parentAiCallLog.retryCount + 1;
        log.parentAiCallLog = parentAiCallLog;
        return log;
    }

    // 외부 요청 없이 partition 결과를 대표하는 log다. provider/model은 해당 run의 확정 계약을 기록한다.
    public static AiCallLog aggregateResult(AiProvider provider, String model, AiExecutionStage stage,
                                            AiInputReferenceType referenceType, String referenceId, String fingerprint,
                                            PromptTemplate template) {
        AiCallLog log = pending(provider, model, stage, referenceType, referenceId, fingerprint, template, null, null);
        log.callRole = AiCallLogRole.AGGREGATE_RESULT;
        return log;
    }

    // prompt 렌더링·입력 제한 실패는 실제 외부 요청이 아니므로 provider 비용 집계와 분리한다.
    public static AiCallLog preparationFailure(AiProvider provider, String model, AiExecutionStage stage,
                                               AiInputReferenceType referenceType, String referenceId, String fingerprint,
                                               PromptTemplate template) {
        AiCallLog log = pending(provider, model, stage, referenceType, referenceId, fingerprint, template, null, null);
        log.callRole = AiCallLogRole.PREPARATION_FAILURE;
        return log;
    }

    public void start() {
        this.status = AiCallLogStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void succeed() {
        this.status = AiCallLogStatus.SUCCEEDED;
        this.valid = true;
        this.errorType = AiCallLogErrorType.NONE;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(AiCallLogErrorType errorType, String errorMessage) {
        this.status = AiCallLogStatus.FAILED;
        this.valid = false;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void recordProviderCompletionMetadata(String metadataJson) {
        this.providerCompletionMetadata = metadataJson;
    }

    public void reuse(AiCallLog sourceCall) {
        this.status = AiCallLogStatus.SUCCEEDED;
        this.valid = true;
        this.reused = true;
        this.reusedFromCall = sourceCall;
        this.errorType = AiCallLogErrorType.NONE;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }
}
