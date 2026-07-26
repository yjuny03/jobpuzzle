package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.config.AiGenerationProperties;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AnthropicClientTest {

    private static final String BASE_URL = "http://anthropic.test";
    private static final String DUMMY_KEY = "test-key-not-a-real-secret";

    private AiGenerationProperties properties;
    private MockRestServiceServer server;
    private AnthropicClient client;

    @BeforeEach
    void setUp() {
        properties = properties();
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AnthropicClient(properties, builder.build(), new ObjectMapper());
    }

    @Test
    void sendsMessagesContractWithStageSpecificModelOutputTokensAndUserPrompt() {
        properties.getAnthropic().getMaxOutputTokens().setJson01(111);
        properties.getAnthropic().getMaxOutputTokens().setJson02(222);
        properties.getAnthropic().getMaxOutputTokens().setJson05(333);

        expectMessage("claude-test-sonnet", 111, "JSON-01 prompt");
        expectMessage("claude-test-sonnet", 222, "JSON-02 prompt");
        expectMessage("claude-test-sonnet", 333, "JSON-05 prompt");

        assertThat(client.analyzeJobPosting("JSON-01 prompt")).isEqualTo("{}");
        assertThat(client.analyzeCandidateMaterial("JSON-02 prompt")).isEqualTo("{}");
        assertThat(client.generateCustomizedAnalysis("JSON-05 prompt")).isEqualTo("{}");
        server.verify();
    }

    @Test
    void concatenatesOnlyTextBlocksInResponseOrder() {
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                .andRespond(withSuccess("""
                        {"content":[
                          {"type":"text","text":"first"},
                          {"type":"tool_use","id":"ignored"},
                          {"type":"text","text":"-second"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.analyzeJobPosting("prompt")).isEqualTo("first-second");
        server.verify();
    }

    @Test
    void responseWithoutTextBlockFailsAsEmptyResponse() {
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                .andRespond(withSuccess("{" + "\"content\":[{\"type\":\"tool_use\"}]}" , MediaType.APPLICATION_JSON));

        assertFailure(() -> client.analyzeJobPosting("prompt"), AiCallLogErrorType.EMPTY_RESPONSE);
        server.verify();
    }

    @Test
    void invalidEnvelopeUsesResponseParseFailureWithoutLeakingKey() {
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.analyzeJobPosting("prompt"))
                .isInstanceOf(AiProcessingException.class)
                .satisfies(error -> {
                    AiProcessingException value = (AiProcessingException) error;
                    assertThat(value.getErrorType()).isEqualTo(AiCallLogErrorType.RESPONSE_PARSE_FAILED);
                    assertThat(value.getMessage()).doesNotContain(DUMMY_KEY);
                });
        server.verify();
    }

    @Test
    void mapsHttpErrorsWithoutLeakingAuthenticationValue() {
        assertHttpFailure(HttpStatus.UNAUTHORIZED, AiCallLogErrorType.PROVIDER_ERROR);
        assertHttpFailure(HttpStatus.FORBIDDEN, AiCallLogErrorType.PROVIDER_ERROR);
        assertHttpFailure(HttpStatus.BAD_REQUEST, AiCallLogErrorType.PROVIDER_ERROR);
        assertHttpFailure(HttpStatus.NOT_FOUND, AiCallLogErrorType.PROVIDER_ERROR);
        assertHttpFailure(HttpStatus.CONFLICT, AiCallLogErrorType.PROVIDER_ERROR);
        assertHttpFailure(HttpStatus.UNPROCESSABLE_ENTITY, AiCallLogErrorType.PROVIDER_ERROR);
        assertHttpFailure(HttpStatus.TOO_MANY_REQUESTS, AiCallLogErrorType.RATE_LIMIT);
        assertHttpFailure(HttpStatus.INTERNAL_SERVER_ERROR, AiCallLogErrorType.PROVIDER_ERROR);
    }

    @Test
    void mapsReadTimeoutToTimeoutWithoutRealNetworkCall() {
        ClientHttpRequestFactory timeoutFactory = (uri, method) -> {
            throw new SocketTimeoutException("dummy timeout");
        };
        AnthropicClient timeoutClient = new AnthropicClient(properties,
                RestClient.builder().baseUrl(BASE_URL).requestFactory(timeoutFactory).build(), new ObjectMapper());

        assertFailure(() -> timeoutClient.analyzeJobPosting("prompt"), AiCallLogErrorType.TIMEOUT);
    }

    private void expectMessage(String model, int maxTokens, String prompt) {
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header("x-api-key", DUMMY_KEY))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(jsonPath("$.model").value(model))
                .andExpect(jsonPath("$.max_tokens").value(maxTokens))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value(prompt))
                .andRespond(withSuccess("{\"content\":[{\"type\":\"text\",\"text\":\"{}\"}]}", MediaType.APPLICATION_JSON));
    }

    private void assertHttpFailure(HttpStatus status, AiCallLogErrorType expected) {
        server.reset();
        server.expect(once(), requestTo(BASE_URL + "/v1/messages"))
                // Dummy key is deliberately returned by the mock body; adapter errors must not expose it.
                .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON).body(DUMMY_KEY));

        assertThatThrownBy(() -> client.analyzeJobPosting("prompt"))
                .isInstanceOf(AiProcessingException.class)
                .satisfies(error -> {
                    AiProcessingException value = (AiProcessingException) error;
                    assertThat(value.getErrorType()).isEqualTo(expected);
                    assertThat(value.getMessage()).doesNotContain(DUMMY_KEY);
                });
        server.verify();
    }

    private void assertFailure(Runnable action, AiCallLogErrorType expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AiProcessingException.class)
                .extracting(error -> ((AiProcessingException) error).getErrorType())
                .isEqualTo(expected);
    }

    private AiGenerationProperties properties() {
        AiGenerationProperties value = new AiGenerationProperties();
        value.getAnthropic().setApiKey(DUMMY_KEY);
        value.getAnthropic().setModel("claude-test-sonnet");
        value.getAnthropic().setApiVersion("2023-06-01");
        return value;
    }
}
