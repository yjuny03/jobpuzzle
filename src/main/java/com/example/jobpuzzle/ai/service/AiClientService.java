package com.example.jobpuzzle.ai.service;

import com.example.jobpuzzle.ai.client.AiClient;
import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.log.AiProvider;
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

    // Provider의 원시 JSON 응답을 호출 계층에 그대로 전달한다.
    public String analyzeJobPosting(String prompt){
        return aiClient.analyzeJobPosting(prompt);
    }

    public String analyzeCandidateMaterial(String prompt) {
        return aiClient.analyzeCandidateMaterial(prompt);
    }

    // JSON-05 Provider 원시 JSON을 후속 파싱·검증 계층으로 전달한다.
    public String generateCustomizedAnalysis(String renderedPrompt) {
        return aiClient.generateCustomizedAnalysis(renderedPrompt);
    }

    public AiProvider getProvider() {
        return aiClient.getProvider();
    }

    public String getModel() {
        return aiClient.getModel();
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
