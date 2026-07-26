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
import java.util.regex.Pattern;

@Component
public class AnthropicClient implements AiClient {

    private static final int PROVIDER_ERROR_DETAIL_LIMIT = 500;
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9_-]{1,100}");

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
            // Provider 원문은 저장하지 않는다. 재현에 필요한 상태·안전한 error 필드·request-id만 AiCallLog에 남긴다.
            throw failure(httpErrorType(exception.getStatusCode().value()), providerFailureMessage(exception));
        } catch (ResourceAccessException exception) {
            throw failure(isTimeout(exception) ? AiCallLogErrorType.TIMEOUT : AiCallLogErrorType.PROVIDER_ERROR,
                    transportFailureMessage(exception));
        } catch (RuntimeException exception) {
            // RestClient 구현체별로 transport 예외가 ResourceAccessException이 아닐 수 있다. 원문 메시지는 남기지 않는다.
            throw failure(AiCallLogErrorType.PROVIDER_ERROR, unexpectedFailureMessage(exception));
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

    private String providerFailureMessage(RestClientResponseException exception) {
        String requestId = exception.getResponseHeaders() == null ? null
                : exception.getResponseHeaders().getFirst("request-id");
        SafeErrorEnvelope envelope = null;
        try {
            envelope = objectMapper.readValue(exception.getResponseBodyAsString(), SafeErrorEnvelope.class);
        } catch (JsonProcessingException ignored) {
            // JSON이 아닌 오류 본문은 기록하지 않는다. provider 원문에는 인증값이나 입력값이 포함될 수 있다.
        }
        String type = sanitize(envelope == null || envelope.error() == null ? null : envelope.error().type());
        String message = sanitize(envelope == null || envelope.error() == null ? null : envelope.error().message());
        String safeRequestId = requestId != null && REQUEST_ID.matcher(requestId).matches() ? requestId : null;

        StringBuilder detail = new StringBuilder("Anthropic request failed; httpStatus=")
                .append(exception.getStatusCode().value());
        if (type != null) detail.append("; providerType=").append(type);
        if (message != null) detail.append("; providerMessage=").append(message);
        if (safeRequestId != null) detail.append("; requestId=").append(safeRequestId);
        return detail.toString();
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return null;
        String safe = value.replaceAll("[\\r\\n\\t]", " ").trim();
        String apiKey = properties.getAnthropic().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) safe = safe.replace(apiKey, "[REDACTED]");
        return safe.isBlank() ? null : safe.substring(0, Math.min(safe.length(), PROVIDER_ERROR_DETAIL_LIMIT));
    }

    private String transportFailureMessage(ResourceAccessException exception) {
        // 네트워크 예외의 원문 메시지는 프록시·인증 정보를 포함할 수 있어 저장하지 않는다.
        Throwable cause = exception.getCause();
        String causeType = cause == null ? null : cause.getClass().getSimpleName();
        return causeType == null || causeType.isBlank()
                ? "Anthropic transport request failed; exception=" + exception.getClass().getSimpleName()
                : "Anthropic transport request failed; exception=" + exception.getClass().getSimpleName()
                + "; cause=" + causeType;
    }

    private String unexpectedFailureMessage(RuntimeException exception) {
        Throwable cause = exception.getCause();
        String detail = "Anthropic request failed; exception=" + exception.getClass().getSimpleName();
        return cause == null ? detail : detail + "; cause=" + cause.getClass().getSimpleName();
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
    private record SafeErrorEnvelope(SafeError error) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SafeError(String type, String message) { }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Response(List<ContentBlock> content) { }

    // Anthropic envelope에는 id·usage·tool input 등 adapter가 소유하지 않는 필드가 있으므로 text 추출에 필요한 값만 읽는다.
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text) { }
}
