package com.example.jobpuzzle.ai.client;

import org.springframework.stereotype.Component;

@Component
public class AnthropicClient implements AiClient {

    @Override
    public String call(String prompt) {
        // TODO: Claude API 호출 구현
        return "{}";
    }
}
