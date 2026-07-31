package com.example.jobpuzzle.analysis.rag.config;

import jakarta.annotation.PostConstruct;
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

    // Qdrant Cloud 용량 보호 설정을 검증한다.
    // 주의: 이 메서드를 Qdrant.Capacity 안에 두면 안 된다. Capacity는 RagProperties에 바인딩되는
    // 순수 값 객체(POJO)일 뿐 별도의 Spring Bean이 아니어서, CommonAnnotationBeanPostProcessor가
    // 그 안의 @PostConstruct를 전혀 처리하지 않는다 — 검증 코드가 조용히 실행되지 않는 버그가 된다.
    // RagProperties 자체는 RagConfiguration의 @EnableConfigurationProperties(RagProperties.class)로
    // 등록된 실제 Bean이므로, 여기서 중첩 값을 꺼내 검증해야 실제로 호출된다.
    @PostConstruct
    void validateQdrantCapacity() {
        Qdrant.Capacity capacity = qdrant.getCapacity();
        // enabled 기본값이 false이므로, 배포에서 명시적으로 켜지 않는 한 아래 검증은 실행되지 않는다.
        // 로컬 개발·기존 테스트가 capacity 설정을 전혀 건드리지 않아도 기동에 영향이 없는 이유다.
        if (!capacity.isEnabled()) {
            return;
        }
        if (capacity.getHardLimit() <= 0) {
            throw new IllegalStateException(
                    "app.rag.qdrant.capacity.hard-limit must be greater than 0 when capacity guard is enabled");
        }
        if (capacity.getWarningThreshold() < 0) {
            throw new IllegalStateException("app.rag.qdrant.capacity.warning-threshold must not be negative");
        }
        if (capacity.getSafetyMargin() < 0) {
            throw new IllegalStateException("app.rag.qdrant.capacity.safety-margin must not be negative");
        }
        if (capacity.getSafetyMargin() >= capacity.getHardLimit()) {
            throw new IllegalStateException("app.rag.qdrant.capacity.safety-margin must be smaller than hard-limit");
        }
        long effectiveLimit = capacity.getHardLimit() - capacity.getSafetyMargin();
        if (capacity.getWarningThreshold() > effectiveLimit) {
            throw new IllegalStateException(
                    "app.rag.qdrant.capacity.warning-threshold must not exceed hard-limit minus safety-margin");
        }
    }

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
        private Capacity capacity = new Capacity();

        // Qdrant Cloud의 실제 point/스토리지 한도는 코드에 상수로 고정하지 않고 배포 환경변수로만
        // 주입한다. enabled 기본값은 false — 명시적으로 켜지 않는 한 QdrantVectorSearchAdapter.index()는
        // 지금까지와 완전히 동일하게 동작한다(락도, /points/count 조회도, 용량 판정도 전혀 타지 않음).
        @Getter
        @Setter
        public static class Capacity {
            private boolean enabled = false;
            private long hardLimit = 0;
            private long warningThreshold = 0;
            private long safetyMargin = 0;
        }
    }
}
