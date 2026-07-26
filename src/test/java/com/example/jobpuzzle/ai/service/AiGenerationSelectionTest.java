package com.example.jobpuzzle.ai.service;

import com.example.jobpuzzle.ai.client.AiClient;
import com.example.jobpuzzle.ai.config.AiGenerationProperties;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiGenerationSelectionTest {

    @Test
    void mockDefaultDoesNotRequireAnthropicConfiguration() {
        AiClient mockClient = mock(AiClient.class);
        AiClient anthropicClient = mock(AiClient.class);
        when(mockClient.getModel()).thenReturn("mock-v1");

        AiClientService service = new AiClientService(mockClient, anthropicClient, new AiGenerationProperties());

        GenerationClientSelection selection = service.resolve(AiExecutionStage.JOB_POSTING_ANALYSIS);

        assertThat(selection.provider()).isEqualTo(AiProvider.MOCK);
        assertThat(selection.client()).isSameAs(mockClient);
        assertThat(selection.model()).isEqualTo("mock-v1");
        assertThat(selection.maxOutputTokens()).isEqualTo(4096);
    }

    @Test
    void e2eStageOverrideSelectsAnthropicAndUsesConfiguredModel() {
        AiClient mockClient = mock(AiClient.class);
        AiClient anthropicClient = mock(AiClient.class);
        AiGenerationProperties properties = new AiGenerationProperties();
        properties.getStageOverrides().put(AiExecutionStage.JOB_POSTING_ANALYSIS, AiProvider.ANTHROPIC);
        properties.getAnthropic().setApiKey("test-key");
        properties.getAnthropic().setModel("claude-test-sonnet");

        AiClientService service = new AiClientService(mockClient, anthropicClient, properties);
        GenerationClientSelection selection = service.resolve(AiExecutionStage.JOB_POSTING_ANALYSIS);

        assertThat(selection.provider()).isEqualTo(AiProvider.ANTHROPIC);
        assertThat(selection.client()).isSameAs(anthropicClient);
        assertThat(selection.model()).isEqualTo("claude-test-sonnet");

        service.analyzeJobPosting(selection, "prompt");
        verify(anthropicClient).analyzeJobPosting("prompt");
    }

    @Test
    void selectedAnthropicRequiresKeyAndModelButMockDoesNot() {
        AiClient mockClient = mock(AiClient.class);
        AiClient anthropicClient = mock(AiClient.class);
        AiGenerationProperties properties = new AiGenerationProperties();
        properties.getStageOverrides().put(AiExecutionStage.CUSTOMIZED_SYNTHESIS, AiProvider.ANTHROPIC);

        AiClientService service = new AiClientService(mockClient, anthropicClient, properties);

        assertThatThrownBy(() -> service.resolve(AiExecutionStage.CUSTOMIZED_SYNTHESIS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API key");
    }
}
