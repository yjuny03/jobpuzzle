package com.example.jobpuzzle.ai.log;

import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 성공 결과는 이 엔티티가 아니라 결과별 테이블이 ai_call_log_id로 이 로그를 참조하는 방향
@Getter
@Entity
@NoArgsConstructor
@Table(name = "ai_call_log", indexes = {
        @Index(name = "idx_execution_stage_fingerprint_status", columnList = "execution_stage, input_fingerprint, status")
})
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

    // 기존 SUCCEEDED 실행 결과를 재사용했는지 여부
    @Column(name = "reused", nullable = false)
    private boolean reused;

    // 재사용한 원본 로그. 동일 input_fingerprint·executionStage의 최초 비재사용 SUCCEEDED 로그를 직접 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reused_from_call_id")
    private AiCallLog reusedFromCallLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id")
    private PromptTemplate promptTemplate;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private JobGuideDocument guide;

    @Column(name = "guide_version", length = 20)
    private String guideVersion;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiCallLogStatus status;

    // 호출 전(PENDING/RUNNING) null, 성공(SUCCEEDED) true, 실패(FAILED) false
    @Column(name = "valid")
    private Boolean valid;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 30)
    private AiCallLogErrorType errorType;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    // 동일 input_fingerprint·executionStage에서 실제 Provider 재호출 횟수. 재사용 성공은 증가시키지 않음
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    // 재시도 전 호출 로그(재시도 이력). reusedFromCallLog(재사용 원본 참조)와는 별개 개념
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_ai_call_log_id")
    private AiCallLog parentAiCallLog;

    @Builder
    private AiCallLog(
            AiProvider provider,
            String model,
            AiExecutionStage executionStage,
            AiInputReferenceType inputReferenceType,
            String inputReferenceId,
            String inputFingerprint,
            PromptTemplate promptTemplate,
            String promptVersion,
            JobGuideDocument guide,
            String guideVersion,
            AiCallLog parentAiCallLog
    ) {
        this.provider = provider;
        this.model = model;
        this.executionStage = executionStage;
        this.inputReferenceType = inputReferenceType;
        this.inputReferenceId = inputReferenceId;
        this.inputFingerprint = inputFingerprint;
        this.promptTemplate = promptTemplate;
        this.promptVersion = promptVersion;
        this.guide = guide;
        this.guideVersion = guideVersion;
        this.parentAiCallLog = parentAiCallLog;

        this.requestedAt = LocalDateTime.now();
        this.status = AiCallLogStatus.PENDING;
        this.errorType = AiCallLogErrorType.NONE;
        this.retryCount = 0;
        this.reused = false;
    }

    public void markRunning() {
        this.status = AiCallLogStatus.RUNNING;
    }

    // 실제 Provider 호출로 성공 종결
    public void complete() {
        this.status = AiCallLogStatus.SUCCEEDED;
        this.valid = true;
        this.errorType = AiCallLogErrorType.NONE;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    // 기존 SUCCEEDED 실행 결과를 재사용하여 성공 종결 (실제 Provider 재호출 없음)
    public void completeAsReused(AiCallLog reusedFromCallLog) {
        this.reused = true;
        this.reusedFromCallLog = reusedFromCallLog;
        complete();
    }

    public void fail(AiCallLogErrorType errorType, String errorMessage) {
        this.status = AiCallLogStatus.FAILED;
        this.valid = false;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }
}