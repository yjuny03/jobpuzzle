package com.example.jobpuzzle.ai.service;

import com.example.jobpuzzle.ai.client.AiClient;
import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.log.AiProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
//@Transactional
public class AiClientService {

    private final AiClient aiClient;

    // app.ai.provider: MOCK(기본값) 또는 ANTHROPIC. 코드 수정 없이 설정만으로 전환
    public AiClientService(
            @Qualifier("mockAiClient") AiClient mockAiClient,
            @Qualifier("anthropicClient") AiClient anthropicClient,
            @Value("${app.ai.provider:MOCK}") String provider
    ) {
        this.aiClient = "ANTHROPIC".equalsIgnoreCase(provider) ? anthropicClient : mockAiClient;
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

    // interview 추가: 호출자는 Mock/Anthropic 구현을 알지 않고 동일한 JSON 계약만 사용한다.
    public String generateBasicQuestions(String renderedPrompt) {
        return aiClient.generateBasicQuestions(renderedPrompt);
    }

    public String generateWeaknessQuestions(String renderedPrompt) {
        return aiClient.generateWeaknessQuestions(renderedPrompt);
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
