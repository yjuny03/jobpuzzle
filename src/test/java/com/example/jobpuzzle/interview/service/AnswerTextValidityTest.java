package com.example.jobpuzzle.interview.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerTextValidityTest {

    @Test
    void rejectsShortFillerAbuseAndRepeatedCharacters() {
        assertThat(AnswerTextValidity.isUnusable("기모띠")).isTrue();
        assertThat(AnswerTextValidity.isUnusable("씨발놈아 닥쳐라")).isTrue();
        assertThat(AnswerTextValidity.isUnusable("ㅋㅋㅋㅋㅋㅋ")).isTrue();
    }

    @Test
    void acceptsAConcreteInterviewAnswer() {
        assertThat(AnswerTextValidity.isUnusable(
                "WebAuthn 검증 실패 시 재시도를 안내하고 서버 로그로 원인을 추적했습니다."
        )).isFalse();
    }
}
