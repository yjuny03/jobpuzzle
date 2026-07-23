package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.dto.AnswerEvaluationResult;
import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.dto.WeaknessAnswerEvaluationResult;
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
    public AnswerEvaluationResult evaluateAnswer(String prompt) {
        return null;
    }

    @Override
    public WeaknessAnswerEvaluationResult evaluateWeaknessAnswer(String prompt) {
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
    public String call(String prompt) {
        // TODO: Claude API 호출 구현
        return "{}";
    }
}
