package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.log.AiProvider;

public interface AiClient {

    String analyzeJobPosting(String prompt);

    String analyzeCandidateMaterial(String prompt);

    String generateCustomizedAnalysis(String renderedPrompt);

    // interview 추가: JSON-11 기본 질문 생성 Provider 계약
    String generateBasicQuestions(String renderedPrompt);

    // interview 추가: JSON-09 약점 보완 질문 생성 Provider 계약
    String generateWeaknessQuestions(String renderedPrompt);

    QuestionGenerationResult generateQuestions(String prompt);

    FinalReportResult finalReport(String prompt);

    AiProvider getProvider();

    String getModel();

    String call(String prompt);
}
