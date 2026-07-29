package com.example.jobpuzzle.guide.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** 관리자 가이드 전처리 전용 OpenAI 설정. 일반 분석 생성·RAG 임베딩 설정과 분리한다. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.guide.preprocessing")
public class GuidePreprocessingProperties {
    private boolean enabled;
    private String apiKey;
    private String baseUrl = "https://api.openai.com";
    private String model = "gpt-5.6-terra";
    private Duration requestTimeout = Duration.ofSeconds(60);
}
