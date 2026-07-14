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

    private String provider;

    private String model;

    private Long promptTemplateId;

    private Integer promptVersion;

    private Long guideId;

    private Integer guideVersion;

    private String resultType;

    private Long resultId;

    private LocalDateTime requestedAt;

    private String status;

    private LocalDateTime completedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Boolean valid;

    private String errorType;

    private Integer retryCount;

}
