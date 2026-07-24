package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestionStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SessionQuestionResponse {
    private Long sessionQuestionId;
    private Long questionId;
    private String questionText;
    private String intent;
    private List<InterviewQuestionEvaluationFocus> evaluationFocus;
    private int displayOrder;
    private InterviewSessionQuestionStatus status;

    public static SessionQuestionResponse from(InterviewSessionQuestion question) {
        return SessionQuestionResponse.builder()
                .sessionQuestionId(question.getSessionQuestionId())
                .questionId(question.getQuestion().getQuestionId())
                .questionText(question.getQuestionTextSnapshot())
                .intent(question.getIntentSnapshot())
                .evaluationFocus(question.getEvaluationFocusSnapshot())
                .displayOrder(question.getDisplayOrder())
                .status(question.getStatus())
                .build();
    }
}
