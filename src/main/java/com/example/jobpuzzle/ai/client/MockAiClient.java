package com.example.jobpuzzle.ai.client;

import org.springframework.stereotype.Component;

@Component
public class MockAiClient implements AiClient {

    @Override
    public String call(String prompt) {
        // TODO: Mock JSON 응답 반환
        return "{}";
    }
}
