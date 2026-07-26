package com.example.jobpuzzle.analysis.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** RAG 구현체 선택과 외부 provider 연결값을 환경별로 분리한다. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private String mode = "fake";
    private Embedding embedding = new Embedding();
    private Duration requestTimeout = Duration.ofSeconds(15);
    private OpenAi openai = new OpenAi();
    private Qdrant qdrant = new Qdrant();

    @Getter
    @Setter
    public static class Embedding {
        private String provider = "openai";
        private String model = "text-embedding-3-small";
        private int dimension;
    }

    @Getter
    @Setter
    public static class OpenAi {
        private String apiKey;
        private String baseUrl = "https://api.openai.com";
    }

    @Getter
    @Setter
    public static class Qdrant {
        private String url;
        private String apiKey;
        // 물리 collection 전체 이름이 아닌 base 이름이다. adapter가 embedding 계약을 붙여 파생한다.
        private String collection;
    }
}
