package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.log.AiProvider;
import org.springframework.stereotype.Component;

@Component
public class AnthropicClient implements AiClient {

    @Override
    public String analyzeJobPosting(String prompt) {
        return "{}";
    }

    @Override
    public String analyzeCandidateMaterial(String prompt) {
        return "{}";
    }

    @Override
    public String generateCustomizedAnalysis(String renderedPrompt) {
        return "{}";
    }

    // interview 추가: 실제 Claude HTTP 연동 시 이 두 메서드의 반환값만 JSON-11/JSON-09로 교체한다.
    @Override
    public String generateBasicQuestions(String renderedPrompt) {
        return call(renderedPrompt);
    }

    @Override
    public String generateWeaknessQuestions(String renderedPrompt) {
        return call(renderedPrompt);
    }

    @Override
    public QuestionGenerationResult generateQuestions(String prompt) {
        return null;
    }

    @Override
    public FinalReportResult finalReport(String prompt) {
        return null;
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.ANTHROPIC;
    }

    @Override
    public String getModel() {
        return "anthropic-unconfigured";
    }

    @Override
    public String call(String prompt) {
        // TODO: Claude API 호출 구현
        return "{}";
    }
}
