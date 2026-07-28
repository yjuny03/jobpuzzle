package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.InterviewQuestion;
import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RemainingQuestionResponse {
    private Long questionId;
    private int displayOrder;
    private String questionText;
    private String intent;
    private List<InterviewQuestionEvaluationFocus> evaluationFocus;

    public static RemainingQuestionResponse from(InterviewQuestion question) {
        return RemainingQuestionResponse.builder()
                .questionId(question.getQuestionId())
                .displayOrder(question.getDisplayOrder())
                .questionText(question.getQuestion())
                .intent(question.getIntent())
                .evaluationFocus(question.getEvaluationFocus())
                .build();
    }
}
