package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.interview.entity.FollowUpQuestionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

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

    /**
     * 선택한 상위 약점 관점 안에서 확인된 하위 진단 키워드입니다.
     * 예: ["역할 설명 부족", "주도성 부족"]
     */
    @Builder.Default
    private List<String> weaknessTags = List.of();

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
