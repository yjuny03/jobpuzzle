package com.example.jobpuzzle.ai.service;

import com.example.jobpuzzle.ai.client.AiClient;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiProvider;

/**
 * 한 실행에서 log·fingerprint·입력 검증·실제 호출이 공유하는 불변 선택 결과다.
 * 호출 중 설정을 다시 해석하지 않아 provider/model과 실제 client가 엇갈리는 것을 막는다.
 */
public record GenerationClientSelection(
        AiExecutionStage stage,
        AiClient client,
        AiProvider provider,
        String model,
        int maxOutputTokens
) {
    public GenerationClientSelection {
        if (stage == null || client == null || provider == null || model == null || model.isBlank() || maxOutputTokens < 1) {
            throw new IllegalArgumentException("generation client selection is incomplete");
        }
    }
}
