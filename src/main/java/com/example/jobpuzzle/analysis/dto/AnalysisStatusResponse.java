package com.example.jobpuzzle.analysis.dto;
import lombok.Builder; import lombok.Getter;
@Getter @Builder public class AnalysisStatusResponse {
    private Long analysisCaseId;
    private Long snapshotId;
    private String analysisCaseStatus;
    private AnalysisStageStatus jobPostingAnalysisStatus;
    private AnalysisStageStatus candidateMaterialAnalysisStatus;
    private AnalysisStageStatus guideContextStatus;
    private AnalysisStageStatus customizedAnalysisStatus;
    private String readinessStatus;
    private Boolean canGenerateQuestions;
    private boolean questionSetAvailable;
    private String latestFailureStage;
    private String latestFailureType;
    private String errorCode;
    private Boolean retryable;
    private String userMessage;
    private Integer attemptCount;
    private Integer maxAttempts;
    private Integer retryAfterSeconds;
    private String traceId;
}
