package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.interview.entity.FollowUpQuestionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaknessAnswerEvaluationResult {

    // 재평가 대상 약점 태그
    private String targetWeaknessTag;

    // 재평가 대상 단일 관점
    private String targetDimension;

    private int currentFollowUpDepth;

    private int score;

    private int passThreshold;

    private String comment;

    // score >= passThreshold
    private boolean passed;

    private FollowUp followUp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FollowUp {
        private int depth;
        private String question;
        private FollowUpQuestionType type;
        private String reason;
    }
}