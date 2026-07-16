package com.example.jobpuzzle.ai.log;

import jakarta.persistence.*;
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
    private Long aiCallLogId;

    private AiProvider provider;

    private String model;

    private Long promptTemplateId;

    private String promptVersion;

    private Long guideId;

    private String guideVersion;

    private AiCallLogResultType resultType;

    private Long resultId;

    private LocalDateTime requestedAt;

    private AiCallLogStatus status;

    private LocalDateTime completedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Boolean valid;

    private AiCallLogErrorType errorType;

    private Integer retryCount;

}
