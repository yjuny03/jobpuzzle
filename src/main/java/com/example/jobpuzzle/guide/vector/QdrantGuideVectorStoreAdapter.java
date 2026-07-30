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

    /** 같은 guideId의 기존 point를 교체하고 현재 청크만 Qdrant에 저장한다. */
    @Override
    public Map<Long, String> index(Long guideId, List<GuideVectorDocument> documents) {
        ensureCollection();
        // 같은 가이드를 다시 색인할 때 과거 point가 검색되지 않도록 해당 guideId 범위를 교체한다.
        callPost("/collections/" + collectionName() + "/points/delete?wait=true",
                Map.of("filter", guideFilter(guideId)));
        List<Map<String, Object>> points = documents.stream()
                .map(document -> Map.of(
                        "id", document.chunkId(),
                        "vector", embeddingProvider.embed(document.content()).vector(),
                        "payload", Map.of("guideId", document.guideId())))
                .toList();
        if (!points.isEmpty()) {
            callPut("/collections/" + collectionName() + "/points?wait=true",
                    Map.of("points", points));
        }
        return documents.stream().collect(Collectors.toMap(
                GuideVectorDocument::chunkId,
                document -> collectionName() + ":" + document.chunkId()));
    }

    /** 분석 검색문을 임베딩하고 선택된 활성 가이드 범위 안에서만 유사 청크를 찾는다. */
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

    /** 설정된 컬렉션을 준비하고 검색 필터로 사용하는 guideId 인덱스만 보장한다. */
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
    }

    /** 모델 차원이나 거리 방식이 다른 컬렉션을 재사용해 검색 결과가 깨지는 것을 막는다. */
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

    /** 다른 버전이나 범위의 point가 검색 결과에 섞이지 않도록 guideId로 격리한다. */
    private Map<String, Object> guideFilter(Long guideId) {
        return Map.of("must", List.of(
                Map.of("key", "guideId", "match", Map.of("value", guideId))));
    }

    /** Qdrant POST 실패를 애플리케이션의 벡터 저장소 오류로 통일한다. */
    private Map<?, ?> callPost(String uri, Object body) {
        try {
            Map<?, ?> response = client.post().uri(uri).body(body).retrieve().body(Map.class);
            return response == null ? Map.of() : response;
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
        }
    }

    /** Qdrant PUT 실패를 애플리케이션의 벡터 저장소 오류로 통일한다. */
    private Map<?, ?> callPut(String uri, Object body) {
        try {
            Map<?, ?> response = client.put().uri(uri).body(body).retrieve().body(Map.class);
            return response == null ? Map.of() : response;
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
        }
    }

    /** 설정값과 임베딩 계약으로 기존부터 사용해 온 가이드 컬렉션 이름을 계산한다. */
    private String collectionName() {
        // 기존 ACTIVE 가이드의 embedding_ref와 실제 Qdrant 위치를 유지하기 위해 이름 계약은 변경하지 않는다.
        String identity = provider() + "|" + model() + "|" + dimension();
        return safe(properties.getCollection(), 80) + "-" + safe(provider(), 24)
                + "-" + safe(model(), 48) + "-d" + dimension() + "-" + shortHash(identity);
    }

    /** Provider와 모델명을 Qdrant 컬렉션 이름에 사용할 수 있는 문자열로 정규화한다. */
    private String safe(String value, int maxLength) {
        String normalized = value == null ? "" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) normalized = "guide";
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    /** 서로 다른 임베딩 계약의 컬렉션명이 충돌하지 않도록 짧은 식별 해시를 만든다. */
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
