package com.example.jobpuzzle.guide.client;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.config.GuidePreprocessingProperties;
import com.example.jobpuzzle.guide.dto.GuidePreprocessingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Responses API와 Structured Outputs를 사용해 관리자 가이드 원문을 구조화한다.
 * Provider 원문 오류와 API 키는 애플리케이션 예외에 포함하지 않는다.
 */
@Component
public class OpenAiGuideStructuringClient implements GuideStructuringClient {

    private static final String SYSTEM_INSTRUCTION = """
            당신은 취업 면접 평가 가이드 편집자입니다.
            입력 원문의 사실과 규칙만 사용해 평가 기준을 구조화하세요.
            원문에 없는 기준, 수치, 질문 또는 사실을 만들어내지 마세요.
            청크는 서로 독립적으로 이해할 수 있게 나누되 원문의 중요한 조건을 생략하지 마세요.
            모든 출력은 제공된 JSON Schema를 정확히 따라야 합니다.
            """;

    private final GuidePreprocessingProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiGuideStructuringClient(
            GuidePreprocessingProperties properties,
            @Qualifier("openAiGuidePreprocessingRestClient") RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public GuidePreprocessingResult structure(String sourceText) {
        validateConfiguration();
        try {
            String envelope = restClient.post()
                    .uri("/v1/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request(sourceText))
                    .retrieve()
                    .body(String.class);
            return parseStructuredOutput(envelope);
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientResponseException | ResourceAccessException exception) {
            throw new CustomException(
                    ErrorCode.GUIDE_PREPROCESSING_FAILED,
                    "OpenAI 가이드 전처리 요청에 실패했습니다.");
        } catch (RuntimeException exception) {
            throw new CustomException(
                    ErrorCode.GUIDE_PREPROCESSING_RESPONSE_INVALID,
                    "OpenAI 가이드 전처리 응답을 처리할 수 없습니다.");
        }
    }

    @Override
    public String model() {
        return properties.getModel();
    }

    private Map<String, Object> request(String sourceText) {
        return Map.of(
                "model", properties.getModel(),
                "store", false,
                "input", List.of(
                        Map.of("role", "system", "content", SYSTEM_INSTRUCTION),
                        Map.of("role", "user", "content", sourceText)
                ),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "job_guide_preprocessing",
                        "strict", true,
                        "schema", outputSchema()
                ))
        );
    }

    private Map<String, Object> outputSchema() {
        Map<String, Object> stringArray = Map.of(
                "type", "array",
                "items", Map.of("type", "string")
        );
        Map<String, Object> chunk = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "content", Map.of("type", "string"),
                        "contentSummary", Map.of("type", "string")
                ),
                "required", List.of("title", "content", "contentSummary")
        );
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "applicableScope", Map.of("type", "string"),
                        "evaluationFocus", stringArray,
                        "evidenceRules", stringArray,
                        "questionDirection", stringArray,
                        "avoidQuestions", stringArray,
                        "chunks", Map.of("type", "array", "minItems", 1, "items", chunk)
                ),
                "required", List.of(
                        "applicableScope", "evaluationFocus", "evidenceRules",
                        "questionDirection", "avoidQuestions", "chunks")
        );
    }

    private GuidePreprocessingResult parseStructuredOutput(String envelope) {
        if (envelope == null || envelope.isBlank()) {
            throw invalidResponse();
        }
        try {
            JsonNode root = objectMapper.readTree(envelope);
            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if ("output_text".equals(content.path("type").asText())
                            && !content.path("text").asText().isBlank()) {
                        return objectMapper.readValue(
                                content.path("text").asText(), GuidePreprocessingResult.class);
                    }
                }
            }
            throw invalidResponse();
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidResponse();
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new CustomException(ErrorCode.GUIDE_PREPROCESSING_DISABLED);
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()
                || properties.getModel() == null || properties.getModel().isBlank()) {
            throw new CustomException(ErrorCode.GUIDE_PREPROCESSING_CONFIG_INVALID);
        }
    }

    private CustomException invalidResponse() {
        return new CustomException(ErrorCode.GUIDE_PREPROCESSING_RESPONSE_INVALID);
    }
}
