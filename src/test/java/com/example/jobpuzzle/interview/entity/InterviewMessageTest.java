package com.example.jobpuzzle.interview.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewMessageTest {

    @Test
    void marksTechnicallyFailedAnswerSeparatelyFromBusinessRejectedAnswer() {
        InterviewMessage answer = InterviewMessage.answer(
                null,
                null,
                AnswerType.ORIGINAL_ANSWER,
                "답변 내용"
        );

        answer.markEvaluationFailed();

        assertThat(answer.getMessageType())
                .isEqualTo(InterviewMessageType.EVALUATION_FAILED_ANSWER);
    }
}
