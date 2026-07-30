package com.example.jobpuzzle.guide.client;

import com.example.jobpuzzle.guide.config.GuidePreprocessingProperties;
import com.example.jobpuzzle.guide.dto.GuidePreprocessingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiGuideStructuringClientTest {

    @Test
    void parsesResponsesApiStructuredOutputText() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GuidePreprocessingProperties properties = new GuidePreprocessingProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel("gpt-5.6-terra");
        OpenAiGuideStructuringClient client = new OpenAiGuideStructuringClient(
                properties, builder.build(), new ObjectMapper());
        server.expect(requestTo("https://api.openai.test/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "output": [{
                            "type": "message",
                            "content": [{
                              "type": "output_text",
                              "text": "{\\"applicableScope\\":\\"백엔드\\",\\"evaluationFocus\\":[\\"직무 적합성\\"],\\"evidenceRules\\":[\\"근거 확인\\"],\\"questionDirection\\":[\\"경험 질문\\"],\\"avoidQuestions\\":[],\\"chunks\\":[{\\"title\\":\\"기준\\",\\"content\\":\\"내용\\",\\"contentSummary\\":\\"요약\\"}]}"
                            }]
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        GuidePreprocessingResult result = client.structure("원문");

        assertThat(result.applicableScope()).isEqualTo("백엔드");
        assertThat(result.chunks()).hasSize(1);
        assertThat(result.chunks().getFirst().title()).isEqualTo("기준");
        server.verify();
    }
}
