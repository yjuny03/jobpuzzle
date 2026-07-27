package com.example.jobpuzzle.ai.config;

import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiE2eProfileConfigurationTest {

    @Test
    void defaultProfileKeepsAllAnalysisGenerationStagesOnMock() {
        AiGenerationProperties properties = new AiGenerationProperties();

        assertThat(properties.providerFor(AiExecutionStage.JOB_POSTING_ANALYSIS)).isEqualTo(AiProvider.MOCK);
        assertThat(properties.providerFor(AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS)).isEqualTo(AiProvider.MOCK);
        assertThat(properties.providerFor(AiExecutionStage.CUSTOMIZED_SYNTHESIS)).isEqualTo(AiProvider.MOCK);
        assertThat(properties.getInputLimits().getJson02PartitionMarkerContentChars()).isEqualTo(12_000);
        assertThat(properties.getInputLimits().getJson02PartitionMaxMarkers()).isEqualTo(8);
    }

    @Test
    void aiE2eProfileOverridesJson01AndJson02ToAnthropic() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        var source = new YamlPropertySourceLoader().load("ai-e2e", new ClassPathResource("application-ai-e2e.yaml")).getFirst();
        environment.getPropertySources().addFirst(source);
        AiGenerationProperties properties = Binder.get(environment)
                .bind("app.ai.generation", Bindable.of(AiGenerationProperties.class))
                .orElseThrow(() -> new IllegalStateException("ai-e2e generation properties are missing"));

        // 파일 이름뿐 아니라 activate 조건도 선언돼 있어 일반 local·test profile에는 자동 적용되지 않는다.
        assertThat(environment.getProperty("spring.config.activate.on-profile")).isEqualTo("ai-e2e");
        assertThat(properties.providerFor(AiExecutionStage.JOB_POSTING_ANALYSIS)).isEqualTo(AiProvider.ANTHROPIC);
        assertThat(properties.providerFor(AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS)).isEqualTo(AiProvider.ANTHROPIC);
        assertThat(properties.providerFor(AiExecutionStage.CUSTOMIZED_SYNTHESIS)).isEqualTo(AiProvider.ANTHROPIC);
        assertThat(properties.thinkingFor(AiExecutionStage.JOB_POSTING_ANALYSIS)).isEqualTo(AiGenerationProperties.ThinkingMode.DISABLED);
        assertThat(properties.thinkingFor(AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS)).isEqualTo(AiGenerationProperties.ThinkingMode.DISABLED);
        assertThat(properties.thinkingFor(AiExecutionStage.CUSTOMIZED_SYNTHESIS)).isEqualTo(AiGenerationProperties.ThinkingMode.DISABLED);
        assertThat(properties.getAnthropic().getRequestTimeout()).isEqualTo(Duration.ofSeconds(180));
        assertThat(properties.getAnthropic().getMaxOutputTokens().getJson01()).isEqualTo(8_192);
        assertThat(properties.getAnthropic().getMaxOutputTokens().getJson02()).isEqualTo(12_288);
        assertThat(properties.getAnthropic().getMaxOutputTokens().getJson05()).isEqualTo(16_384);
        // E2E에서만 작은 partition을 강제하고, 일반 profile 기본값은 위 테스트에서 보호한다.
        assertThat(properties.getInputLimits().getJson02PartitionMarkerContentChars()).isEqualTo(4_500);
        assertThat(properties.getInputLimits().getJson02PartitionMaxMarkers()).isEqualTo(2);
        assertThat(properties.getInputLimits().getJson05RetrievalChars()).isEqualTo(50_000);
        assertThat(properties.getInputLimits().getJson05RenderedPromptChars()).isEqualTo(120_000);
    }
}
