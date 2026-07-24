package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.log.AiProvider;

public interface AiClient {

    String analyzeJobPosting(String prompt);

    String analyzeCandidateMaterial(String prompt);

    String generateCustomizedAnalysis(String renderedPrompt);

    QuestionGenerationResult generateQuestions(String prompt);

    FinalReportResult finalReport(String prompt);

    AiProvider getProvider();

    String getModel();

    String call(String prompt);
}
