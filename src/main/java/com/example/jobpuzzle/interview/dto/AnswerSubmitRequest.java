package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.AnswerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnswerSubmitRequest {

    @NotBlank
    private String messageText;

    @NotNull
    private AnswerType answerType;

    private Long parentQuestionMessageId;
}
