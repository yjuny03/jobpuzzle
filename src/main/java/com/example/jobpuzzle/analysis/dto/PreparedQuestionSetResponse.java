package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.interview.entity.QuestionSet;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PreparedQuestionSetResponse {
    private Long questionSetId;
    private String mode;
    private String targetWeaknessTag;
    private int questionCount;
    private LocalDateTime createdAt;

    public static PreparedQuestionSetResponse from(QuestionSet set, int questionCount) {
        return PreparedQuestionSetResponse.builder()
                .questionSetId(set.getQuestionSetId())
                .mode(set.getInterviewMode().name())
                .targetWeaknessTag(set.getTargetWeaknessTag())
                .questionCount(questionCount)
                .createdAt(set.getCreatedAt())
                .build();
    }
}
