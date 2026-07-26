package com.example.jobpuzzle.ai.client;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 실제 Claude API 연동. api-key가 비어있으면 호출 시점에 에러
// 모든 메서드가 call(prompt) 하나로 수렴 — String 반환 메서드는 원시 텍스트를 그대로,
// 타입 반환 메서드는 그 텍스트를 JSON으로 파싱해서 돌려준다.
@Component
public class AnthropicClient implements AiClient {

    private static final Pattern JSON_CODE_BLOCK = Pattern.compile("^```(?:json)?\\s*\\n([\\s\\S]*)\\n?```$");

    private final String apiKey;
    private final String model;
    private final long maxTokens;
    private final ObjectMapper objectMapper;
    private volatile com.anthropic.client.AnthropicClient sdkClient;

    public AnthropicClient(
            @Value("${app.ai.anthropic.api-key:}") String apiKey,
            @Value("${app.ai.anthropic.model:claude-sonnet-5}") String model,
            @Value("${app.ai.anthropic.max-tokens:8000}") long maxTokens,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.objectMapper = objectMapper;
    }

    @Override
    public String analyzeJobPosting(String prompt) {
        return call(prompt);
    }

    @Override
    public String analyzeCandidateMaterial(String prompt) {
        return call(prompt);
    }

    @Override
    public String generateCustomizedAnalysis(String renderedPrompt) {
        return call(renderedPrompt);
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
        return parseJson(call(prompt), QuestionGenerationResult.class);
    }

    @Override
    public FinalReportResult finalReport(String prompt) {
        return parseJson(call(prompt), FinalReportResult.class);
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.ANTHROPIC;
    }

    @Override
    public String getModel() {
        return model;
    }

    // Claude에 프롬프트 하나를 보내고 텍스트 응답을 그대로 반환.
    // 다른 모든 메서드가 거치는 실제 연결 지점.
    @Override
    public String call(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .addUserMessage(prompt)
                .build();

        Message response;
        try {
            response = client().messages().create(params);
        } catch (RateLimitException exception) {
            throw new AiProcessingException(AiCallLogErrorType.RATE_LIMIT, summarize(exception));
        } catch (AnthropicServiceException exception) {
            throw new AiProcessingException(AiCallLogErrorType.PROVIDER_ERROR, summarize(exception));
        }

        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining());

        if (text.isBlank()) {
            throw new AiProcessingException(AiCallLogErrorType.EMPTY_RESPONSE, "Claude returned no text content");
        }
        return text;
    }

    private com.anthropic.client.AnthropicClient client() {
        if (sdkClient == null) {
            synchronized (this) {
                if (sdkClient == null) {
                    if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException(
                                "app.ai.anthropic.api-key가 설정되지 않았습니다. "
                                        + "application-local.yaml에서 ANTHROPIC_API_KEY 환경변수를 채워주세요.");
                    }
                    sdkClient = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
                }
            }
        }
        return sdkClient;
    }

    private <T> T parseJson(String rawText, Class<T> type) {
        String normalized = stripCodeFence(rawText.trim());
        try {
            return objectMapper.readValue(normalized, type);
        } catch (JsonProcessingException exception) {
            throw new AiProcessingException(AiCallLogErrorType.JSON_PARSE_FAIL,
                    "Claude 응답 JSON 파싱 실패: " + exception.getClass().getSimpleName());
        }
    }

    private String stripCodeFence(String text) {
        Matcher matcher = JSON_CODE_BLOCK.matcher(text);
        return matcher.matches() ? matcher.group(1).trim() : text;
    }

    private String summarize(RuntimeException exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}