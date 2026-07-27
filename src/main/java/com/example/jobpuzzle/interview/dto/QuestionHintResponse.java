package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.InterviewQuestion;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionHintResponse {
    private Long questionId;
    private String intent;
    private Object evaluationFocus;
    private Object sourceRefs;

    public static QuestionHintResponse from(InterviewQuestion value) {
        return QuestionHintResponse.builder()
                .questionId(value.getQuestionId())
                .intent(value.getIntent())
                .evaluationFocus(value.getEvaluationFocus())
                .sourceRefs(value.getSourceRefs())
                .build();
    }
}
