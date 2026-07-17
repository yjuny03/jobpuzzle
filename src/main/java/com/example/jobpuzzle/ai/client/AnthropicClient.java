package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.log.AiProvider;
import org.springframework.stereotype.Component;

@Component
public class AnthropicClient implements AiClient {

    @Override
    public JobPostingAnalysisResult analyzeJobPosting(String prompt) {
        return null;
    }

    @Override
    public CandidateMaterialAnalysisResult analyzeCandidateMaterial(String prompt) {
        return null;
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
        return null;
    }

    @Override
    public String call(String prompt) {
        // TODO: Claude API 호출 구현
        return "{}";
    }
}
