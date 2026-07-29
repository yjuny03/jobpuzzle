package com.example.jobpuzzle.guide.vector;

import com.example.jobpuzzle.analysis.rag.embedding.EmbeddingProvider;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.config.GuideVectorProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/** 가이드 청크를 사용자 자료와 분리된 Qdrant collection에 저장하고 guideId로 격리 검색한다. */
@Component
@ConditionalOnProperty(prefix = "app.rag", name = "mode", havingValue = "real")
public class QdrantGuideVectorStoreAdapter implements GuideVectorStorePort {

    private final GuideVectorProperties properties;
    private final EmbeddingProvider embeddingProvider;
    private final RestClient client;

    public QdrantGuideVectorStoreAdapter(
            GuideVectorProperties properties,
            EmbeddingProvider embeddingProvider,
            @Qualifier("qdrantRestClient") RestClient client
    ) {
        this.properties = properties;
        this.embeddingProvider = embeddingProvider;
        this.client = client;
    }

    @Override
    public Map<Long, String> index(Long guideId, List<GuideVectorDocument> documents) {
        ensureCollection();
        // 같은 가이드의 재전처리로 생성된 과거 point가 검색되지 않도록 범위 전체를 먼저 교체한다.
        callPost("/collections/" + collectionName() + "/points/delete",
                Map.of("filter", guideFilter(guideId)));
        List<Map<String, Object>> points = documents.stream()
                .map(document -> Map.of(
                        "id", document.chunkId(),
                        "vector", embeddingProvider.embed(document.content()).vector(),
                        "payload", payload(document)))
                .toList();
        if (!points.isEmpty()) {
            callPut("/collections/" + collectionName() + "/points?wait=true",
                    Map.of("points", points));
        }
        return documents.stream().collect(Collectors.toMap(
                GuideVectorDocument::chunkId,
                document -> collectionName() + ":" + document.chunkId()));
    }

    @Override
    public List<GuideVectorHit> search(Long guideId, String queryText, int topK) {
        Map<?, ?> response = callPost(
                "/collections/" + collectionName() + "/points/query",
                Map.of(
                        "query", embeddingProvider.embed(queryText).vector(),
                        "limit", topK,
                        "with_payload", false,
                        "filter", guideFilter(guideId)));
        List<?> points = response.get("result") instanceof Map<?, ?> result
                && result.get("points") instanceof List<?> values ? values : List.of();
        List<GuideVectorHit> hits = new ArrayList<>();
        for (Object point : points) {
            if (!(point instanceof Map<?, ?> value)
                    || !(value.get("score") instanceof Number score)) {
                throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
            }
            hits.add(new GuideVectorHit(
                    Long.valueOf(String.valueOf(value.get("id"))), score.doubleValue()));
        }
        return hits;
    }

    @Override public String provider() { return embeddingProvider.getProviderName(); }
    @Override public String model() { return embeddingProvider.getModelName(); }
    @Override public int dimension() { return embeddingProvider.getDimension(); }

    private void ensureCollection() {
        try {
            Map<?, ?> response =
                    client.get().uri("/collections/" + collectionName()).retrieve().body(Map.class);
            validateCollection(response);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
            }
            callPut("/collections/" + collectionName(), Map.of(
                    "vectors", Map.of(
                            "size", embeddingProvider.getDimension(),
                            "distance", "Cosine")));
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
        }
        callPut("/collections/" + collectionName() + "/index",
                Map.of("field_name", "guideId", "field_schema", "integer"));
        for (String field : List.of("guideCode", "guideVersion", "embeddingProvider", "embeddingModel")) {
            callPut("/collections/" + collectionName() + "/index",
                    Map.of("field_name", field, "field_schema", "keyword"));
        }
    }

    /** 같은 이름의 collection이 다른 embedding 차원으로 생성된 경우 조용히 재사용하지 않는다. */
    private void validateCollection(Map<?, ?> response) {
        Object result = response == null ? null : response.get("result");
        Object config = result instanceof Map<?, ?> value ? value.get("config") : null;
        Object params = config instanceof Map<?, ?> value ? value.get("params") : null;
        Object vectors = params instanceof Map<?, ?> value ? value.get("vectors") : null;
        if (!(vectors instanceof Map<?, ?> value)
                || !(value.get("size") instanceof Number size)
                || size.intValue() != dimension()
                || !"COSINE".equalsIgnoreCase(String.valueOf(value.get("distance")))) {
            throw new CustomException(ErrorCode.VECTOR_COLLECTION_INCOMPATIBLE);
        }
    }

    private Map<String, Object> payload(GuideVectorDocument document) {
        return Map.of(
                "guideId", document.guideId(),
                "guideCode", document.guideCode(),
                "guideVersion", document.guideVersion(),
                "chunkIndex", document.chunkIndex(),
                "embeddingProvider", provider(),
                "embeddingModel", model(),
                "embeddingDimension", dimension());
    }

    private Map<String, Object> guideFilter(Long guideId) {
        return Map.of("must", List.of(
                Map.of("key", "guideId", "match", Map.of("value", guideId))));
    }

    private Map<?, ?> callPost(String uri, Object body) {
        try {
            Map<?, ?> response = client.post().uri(uri).body(body).retrieve().body(Map.class);
            return response == null ? Map.of() : response;
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
        }
    }

    private Map<?, ?> callPut(String uri, Object body) {
        try {
            Map<?, ?> response = client.put().uri(uri).body(body).retrieve().body(Map.class);
            return response == null ? Map.of() : response;
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
        }
    }

    private String collectionName() {
        String identity = provider() + "|" + model() + "|" + dimension();
        return safe(properties.getCollection(), 80) + "-" + safe(provider(), 24)
                + "-" + safe(model(), 48) + "-d" + dimension() + "-" + shortHash(identity);
    }

    private String safe(String value, int maxLength) {
        String normalized = value == null ? "" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) normalized = "guide";
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(Arrays.copyOf(digest, 6));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
