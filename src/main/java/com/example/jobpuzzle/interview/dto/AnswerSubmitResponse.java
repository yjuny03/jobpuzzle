package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.InterviewSessionStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnswerSubmitResponse {
    private Long answerMessageId;
    private Long evaluationId;
    private Integer score;
    private String summary;
    private boolean evaluationFailed;
    private Long followUpQuestionMessageId;
    private String followUpQuestion;
    private InterviewSessionStatus sessionStatus;
    private SessionQuestionResponse nextQuestion;
}
