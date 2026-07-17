package com.example.jobpuzzle.ai.log;

import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "ai_call_log")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id")
    private PromptTemplate promptTemplate;

    // 호출 시 사용한 프롬프트 버전 스냅샷
    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private JobGuideDocument guide;

    // 호출 시 사용한 가이드 버전 스냅샷
    @Column(name = "guide_version", length = 20)
    private String guideVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 50)
    private AiCallLogResultType resultType;

    // resultType에 따라 서로 다른 결과 테이블의 PK 참조
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiCallLogStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // 호출 전 null, 검증 성공 true, 검증 실패 false
    @Column(name = "valid")
    private Boolean valid;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 30)
    private AiCallLogErrorType errorType;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Builder
    private AiCallLog(
            AiProvider provider,
            String model,
            PromptTemplate promptTemplate,
            String promptVersion,
            JobGuideDocument guide,
            String guideVersion,
            AiCallLogResultType resultType
    ) {
        this.provider = provider;
        this.model = model;
        this.promptTemplate = promptTemplate;
        this.promptVersion = promptVersion;
        this.guide = guide;
        this.guideVersion = guideVersion;
        this.resultType = resultType;

        this.requestedAt = LocalDateTime.now();
        this.status = AiCallLogStatus.REQUESTED;
        this.errorType = AiCallLogErrorType.NONE;
        this.retryCount = 0;
        this.valid = null;
    }

    // AI 응답 검증 및 결과 저장 완료
    public void complete(Long resultId) {
        this.resultId = resultId;
        this.status = AiCallLogStatus.SUCCESS;
        this.valid = true;
        this.errorType = AiCallLogErrorType.NONE;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    // 호출·파싱·검증 실패
    public void fail(
            AiCallLogErrorType errorType,
            String errorMessage
    ) {
        this.status = AiCallLogStatus.FAILED;
        this.valid = false;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    // 동일 호출 재시도 횟수 증가
    public void increaseRetryCount() {
        this.retryCount++;
    }
}