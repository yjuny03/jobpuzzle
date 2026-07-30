package com.example.jobpuzzle.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityRequestClassifierTest {

    // 단수 액션플랜 API는 인증 실패 시 JSON 401 응답 대상으로 분류한다.
    @Test
    void classifiesActionPlanApiAsFunctionRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobpuzzle/action-plan");
        request.setContextPath("/jobpuzzle");

        assertThat(SecurityRequestClassifier.isFunctionRequest(request)).isTrue();
    }

    // 복수형 액션플랜 화면은 로그인 후 원래 화면 복귀가 가능한 일반 화면 요청으로 유지한다.
    @Test
    void keepsActionPlansPageAsViewRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobpuzzle/action-plans");
        request.setContextPath("/jobpuzzle");

        assertThat(SecurityRequestClassifier.isFunctionRequest(request)).isFalse();
    }
}
