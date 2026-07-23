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
