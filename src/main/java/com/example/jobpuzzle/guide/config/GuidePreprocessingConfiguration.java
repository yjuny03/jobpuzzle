package com.example.jobpuzzle.guide.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** 가이드 전처리 전용 HTTP 클라이언트를 구성한다. API 키는 로그나 예외에 포함하지 않는다. */
@Configuration
@EnableConfigurationProperties({GuidePreprocessingProperties.class, GuideVectorProperties.class})
public class GuidePreprocessingConfiguration {

    @Bean("openAiGuidePreprocessingRestClient")
    RestClient openAiGuidePreprocessingRestClient(GuidePreprocessingProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getRequestTimeout());
        requestFactory.setReadTimeout(properties.getRequestTimeout());
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }
}
