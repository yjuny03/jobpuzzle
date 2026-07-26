package com.example.jobpuzzle.analysis.rag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfiguration {
    @Bean("openAiEmbeddingRestClient") @ConditionalOnProperty(prefix="app.rag", name="mode", havingValue="real")
    RestClient openAiEmbeddingRestClient(RagProperties p) { return RestClient.builder().baseUrl(p.getOpenai().getBaseUrl()).requestFactory(requestFactory(p)).defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer "+p.getOpenai().getApiKey()).build(); }
    @Bean("qdrantRestClient") @ConditionalOnProperty(prefix="app.rag", name="mode", havingValue="real")
    RestClient qdrantRestClient(RagProperties p) { RestClient.Builder b=RestClient.builder().baseUrl(p.getQdrant().getUrl()).requestFactory(requestFactory(p)); if(p.getQdrant().getApiKey()!=null&&!p.getQdrant().getApiKey().isBlank()) b.defaultHeader("api-key",p.getQdrant().getApiKey()); return b.build(); }
    private SimpleClientHttpRequestFactory requestFactory(RagProperties p) { SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory(); f.setConnectTimeout(p.getRequestTimeout()); f.setReadTimeout(p.getRequestTimeout()); return f; }
}
