package com.example.jobpuzzle.interview.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class WeaknessTagResponse {
    private String tag;
    private String displayName;
    private String description;
    private int occurrenceCount;
    private int unresolvedCount;
    private int recentAttemptCount;
    private List<Occurrence> recentOccurrences;

    @Getter
    @Builder
    public static class Occurrence {
        private Long sessionId;
        private Long evaluationId;
        private String mode;
        private Integer score;
        private LocalDateTime occurredAt;
        private String status;
        private String resultLabel;
        private List<String> diagnostics;
        private List<Attempt> attempts;
    }

    @Getter
    @Builder
    public static class Attempt {
        private Long sessionId;
        private Long evaluationId;
        private Integer score;
        private LocalDateTime occurredAt;
        private String status;
        private String resultLabel;
        private List<String> diagnostics;
    }
}
