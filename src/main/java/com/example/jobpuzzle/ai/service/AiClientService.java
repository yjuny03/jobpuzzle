package com.example.jobpuzzle.ai.service;

import com.example.jobpuzzle.ai.client.AiClient;
import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
//@Transactional
public class AiClientService {

    private final AiClient aiClient;

    public AiClientService(
            @Qualifier("mockAiClient") AiClient aiClient
    ) {
        this.aiClient = aiClient;
    }

    public JobPostingAnalysisResult analyzeJobPosting(String prompt){
        return aiClient.analyzeJobPosting(prompt);
    }

    public CandidateMaterialAnalysisResult analyzeCandidateMaterial(String prompt) {
        return aiClient.analyzeCandidateMaterial(prompt);
    }

    public QuestionGenerationResult generateQuestions(String prompt) {
        return aiClient.generateQuestions(prompt);
    }

    public FinalReportResult finalReport(String prompt) { return aiClient.finalReport(prompt);}

    public void callAi() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void saveAiResult() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void validateAiResponse() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void handleAiFailureFallback() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void logAiCall() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void getActivePromptTemplate() {
        // TODO: 클래스 정의서 기준으로 구현
    }
}
