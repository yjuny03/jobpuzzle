package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.config.AiGenerationProperties;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.List;

@Component
public class AnthropicClient implements AiClient {

    private final AiGenerationProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AnthropicClient(
            AiGenerationProperties properties,
            @Qualifier("anthropicRestClient") RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String analyzeJobPosting(String prompt) {
        return message(AiExecutionStage.JOB_POSTING_ANALYSIS, prompt);
    }

    @Override
    public String analyzeCandidateMaterial(String prompt) {
        return message(AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS, prompt);
    }

    @Override
    public String generateCustomizedAnalysis(String renderedPrompt) {
        return message(AiExecutionStage.CUSTOMIZED_SYNTHESIS, renderedPrompt);
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
        return properties.getAnthropic().getModel();
    }

    @Override
    public String call(String prompt) {
        return message(AiExecutionStage.JOB_POSTING_ANALYSIS, prompt);
    }

    /**
     * Messages API의 text block만 기존 parser에 전달한다. JSON 파싱과 DTO/근거 검증은 이 adapter가 담당하지 않는다.
     * 인증값과 Provider 원시 오류 본문은 예외 메시지·로그에 포함하지 않는다.
     */
    private String message(AiExecutionStage stage, String prompt) {
        validateSelectedConfiguration();
        if (prompt == null || prompt.isBlank()) throw failure(AiCallLogErrorType.EMPTY_RESPONSE, "AI prompt is empty");

        AiGenerationProperties.Anthropic anthropic = properties.getAnthropic();
        Request request = new Request(
                anthropic.getModel(),
                anthropic.getMaxOutputTokens().forStage(stage),
                List.of(new Message("user", prompt))
        );

        final String rawEnvelope;
        try {
            rawEnvelope = restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", anthropic.getApiKey())
                    .header("anthropic-version", anthropic.getApiVersion())
                    .body(request)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw failure(httpErrorType(exception.getStatusCode().value()), "Anthropic request failed");
        } catch (ResourceAccessException exception) {
            throw failure(isTimeout(exception) ? AiCallLogErrorType.TIMEOUT : AiCallLogErrorType.PROVIDER_ERROR,
                    isTimeout(exception) ? "Anthropic request timed out" : "Anthropic request failed");
        } catch (RuntimeException exception) {
            throw failure(AiCallLogErrorType.PROVIDER_ERROR, "Anthropic request failed");
        }

        if (rawEnvelope == null || rawEnvelope.isBlank()) {
            throw failure(AiCallLogErrorType.EMPTY_RESPONSE, "Anthropic response is empty");
        }
        try {
            Response response = objectMapper.readValue(rawEnvelope, Response.class);
            String text = response.content() == null ? "" : response.content().stream()
                    // tool_use 등 비text block은 실행하거나 해석하지 않는다. text가 없으면 EMPTY_RESPONSE로 끝낸다.
                    .filter(block -> block != null && "text".equals(block.type()) && block.text() != null)
                    .map(ContentBlock::text)
                    .reduce("", String::concat);
            if (text.isBlank()) throw failure(AiCallLogErrorType.EMPTY_RESPONSE, "Anthropic response has no text block");
            return text;
        } catch (AiProcessingException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw failure(AiCallLogErrorType.RESPONSE_PARSE_FAILED, "Anthropic response envelope is invalid");
        }
    }

    private void validateSelectedConfiguration() {
        AiGenerationProperties.Anthropic anthropic = properties.getAnthropic();
        if (anthropic.getApiKey() == null || anthropic.getApiKey().isBlank()
                || anthropic.getModel() == null || anthropic.getModel().isBlank()
                || anthropic.getApiVersion() == null || anthropic.getApiVersion().isBlank()) {
            throw failure(AiCallLogErrorType.PROVIDER_ERROR, "Anthropic configuration is incomplete");
        }
    }

    private AiCallLogErrorType httpErrorType(int status) {
        if (status == 429) return AiCallLogErrorType.RATE_LIMIT;
        // 400·401·403·404·409·422와 5xx 모두 공개 API 세분화 전에는 provider 오류로 기록한다.
        return AiCallLogErrorType.PROVIDER_ERROR;
    }

    private boolean isTimeout(Throwable value) {
        Throwable current = value;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof InterruptedIOException) return true;
            current = current.getCause();
        }
        return false;
    }

    private AiProcessingException failure(AiCallLogErrorType errorType, String message) {
        return new AiProcessingException(errorType, message);
    }

    private record Request(String model, @JsonProperty("max_tokens") int maxTokens, List<Message> messages) { }
    private record Message(String role, String content) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Response(List<ContentBlock> content) { }

    // Anthropic envelope에는 id·usage·tool input 등 adapter가 소유하지 않는 필드가 있으므로 text 추출에 필요한 값만 읽는다.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text) { }
}
