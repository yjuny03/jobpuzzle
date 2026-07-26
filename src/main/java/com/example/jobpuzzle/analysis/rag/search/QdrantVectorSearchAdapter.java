package com.example.jobpuzzle.analysis.rag.search;

import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.analysis.rag.config.RagProperties;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchRequest;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchResult;
import com.example.jobpuzzle.analysis.rag.embedding.EmbeddingProvider;
import com.example.jobpuzzle.analysis.rag.index.VectorIndexPort;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Qdrant point의 payload filter를 DB retrieval 범위와 동일하게 강제한다. */
@Service
@ConditionalOnProperty(prefix = "app.rag", name = "mode", havingValue = "real")
public class QdrantVectorSearchAdapter implements VectorSearchPort, VectorIndexPort {

    private final RagProperties properties;
    private final EmbeddingProvider embeddingProvider;
    private final AnalysisMaterialChunkRepository chunkRepository;
    private final RestClient client;

    public QdrantVectorSearchAdapter(
            RagProperties properties,
            EmbeddingProvider embeddingProvider,
            AnalysisMaterialChunkRepository chunkRepository,
            @Qualifier("qdrantRestClient") RestClient client
    ) {
        this.properties = properties;
        this.embeddingProvider = embeddingProvider;
        this.chunkRepository = chunkRepository;
        this.client = client;
    }

    @Override
    public List<AnalysisMaterialChunkSearchResult> search(AnalysisMaterialChunkSearchRequest request) {
        Map<String, Object> body = Map.of(
                "query", embeddingProvider.embed(request.queryText()).vector(),
                "limit", request.topK(),
                "with_payload", true,
                "filter", Map.of("must", List.of(
                        equalsFilter("userId", request.userId()),
                        equalsFilter("snapshotId", request.snapshotId()),
                        equalsFilter("chunkingVersion", request.chunkingVersion()),
                        Map.of("key", "documentType", "match", Map.of(
                                "any", request.allowedDocumentTypes().stream().map(Enum::name).toList()
                        ))
                ))
        );

        List<?> points = points(callPost("/collections/" + collectionName() + "/points/query", body));
        List<AnalysisMaterialChunkSearchResult> results = new ArrayList<>();
        int rank = 1;

        for (Object point : points) {
            Map<?, ?> value = asMap(point);
            Long chunkId = Long.valueOf(String.valueOf(value.get("id")));
            AnalysisMaterialChunk chunk = chunkRepository.findById(chunkId)
                    .orElseThrow(() -> new CustomException(ErrorCode.JSON05_RESULT_INTEGRITY_CONFLICT));
            results.add(new AnalysisMaterialChunkSearchResult(
                    chunk.getChunkId(), chunk.getSnapshotSource().getSnapshotSourceId(), chunk.getDocumentId(),
                    chunk.getDocumentType(), chunk.getPageStart(), chunk.getPageEnd(), chunk.getCharStart(),
                    chunk.getCharEnd(), chunk.getChunkIndex(), chunk.getContent(),
                    ((Number) value.get("score")).doubleValue(), rank++
            ));
        }
        return results;
    }

    @Override
    public void index(Collection<AnalysisMaterialChunk> chunks) {
        ensureCollection();

        // 한 scope의 chunk ID를 한 번에 조회한다. payload 계약이 모두 같은 point만 재사용한다.
        Map<Long, Map<?, ?>> indexedPayloads = findIndexedPayloads(chunks);
        List<Map<String, Object>> pointsToUpsert = chunks.stream()
                .filter(chunk -> !matchesPayload(indexedPayloads.get(chunk.getChunkId()), chunk))
                .map(chunk -> Map.of(
                        "id", chunk.getChunkId(),
                        "vector", embeddingProvider.embed(chunk.getContent()).vector(),
                        "payload", payload(chunk)
                ))
                .toList();

        if (!pointsToUpsert.isEmpty()) {
            callPut("/collections/" + collectionName() + "/points", Map.of("points", pointsToUpsert));
        }
    }

    @Override
    public void delete(Collection<AnalysisMaterialChunk> chunks) {
        if (!chunks.isEmpty()) {
            callPost("/collections/" + collectionName() + "/points/delete", Map.of(
                    "points", chunks.stream().map(AnalysisMaterialChunk::getChunkId).toList()
            ));
        }
    }

    @Override
    public void requireReady(Long userId, Long snapshotId, Collection<AnalysisMaterialChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new CustomException(ErrorCode.VECTOR_INDEX_NOT_READY);
        }

        Set<Long> expectedIds = chunks.stream()
                .map(AnalysisMaterialChunk::getChunkId)
                .collect(Collectors.toSet());
        Map<String, Object> body = Map.of(
                "limit", expectedIds.size(),
                "with_payload", true,
                "with_vector", false,
                "filter", Map.of("must", List.of(
                        equalsFilter("userId", userId),
                        equalsFilter("snapshotId", snapshotId),
                        Map.of("has_id", expectedIds)
                ))
        );

        List<?> points = points(callPost("/collections/" + collectionName() + "/points/scroll", body));
        if (points.size() != expectedIds.size()) {
            throw new CustomException(ErrorCode.VECTOR_INDEX_NOT_READY);
        }

        Map<Long, AnalysisMaterialChunk> chunksById = chunks.stream().collect(Collectors.toMap(
                AnalysisMaterialChunk::getChunkId,
                chunk -> chunk
        ));
        for (Object point : points) {
            Map<?, ?> value = asMap(point);
            Long id = Long.valueOf(String.valueOf(value.get("id")));
            AnalysisMaterialChunk chunk = chunksById.remove(id);
            if (chunk == null || !matchesPayload(asMapOrNull(value.get("payload")), chunk)) {
                throw new CustomException(ErrorCode.VECTOR_INDEX_NOT_READY);
            }
        }
        if (!chunksById.isEmpty()) {
            throw new CustomException(ErrorCode.VECTOR_INDEX_NOT_READY);
        }
    }

    @Override
    public String getEmbeddingProviderName() {
        return embeddingProvider.getProviderName();
    }

    @Override
    public String getEmbeddingModelName() {
        return embeddingProvider.getModelName();
    }

    /**
     * collection base name에 embedding 계약을 포함한다. 같은 dimension이라도 모델이 다르면
     * 다른 physical collection을 사용하며, 안전한 토큰과 해시를 함께 사용해 이름 충돌을 막는다.
     */
    String collectionName() {
        String base = properties.getQdrant().getCollection();
        String provider = embeddingProvider.getProviderName();
        String model = embeddingProvider.getModelName();
        int dimension = embeddingProvider.getDimension();
        if (isBlank(base) || isBlank(provider) || isBlank(model) || dimension <= 0) {
            throw new CustomException(ErrorCode.VECTOR_COLLECTION_INCOMPATIBLE, "invalid vector collection identity");
        }
        String identity = provider + "|" + model + "|" + dimension;
        return safeToken(base, 80) + "-" + safeToken(provider, 24) + "-" + safeToken(model, 48)
                + "-d" + dimension + "-" + shortHash(identity);
    }

    private Map<Long, Map<?, ?>> findIndexedPayloads(Collection<AnalysisMaterialChunk> chunks) {
        Map<IndexScope, List<AnalysisMaterialChunk>> byScope = chunks.stream().collect(Collectors.groupingBy(
                chunk -> new IndexScope(chunk.getUserId(), chunk.getSnapshot().getSnapshotId(), chunk.getDocumentType())
        ));
        Map<Long, Map<?, ?>> payloads = new HashMap<>();

        for (Map.Entry<IndexScope, List<AnalysisMaterialChunk>> entry : byScope.entrySet()) {
            IndexScope scope = entry.getKey();
            Set<Long> ids = entry.getValue().stream().map(AnalysisMaterialChunk::getChunkId).collect(Collectors.toSet());
            Map<String, Object> body = Map.of(
                    "limit", ids.size(),
                    "with_payload", true,
                    "with_vector", false,
                    "filter", Map.of("must", List.of(
                            equalsFilter("userId", scope.userId()),
                            equalsFilter("snapshotId", scope.snapshotId()),
                            equalsFilter("documentType", scope.documentType().name()),
                            Map.of("has_id", ids)
                    ))
            );
            for (Object point : points(callPost("/collections/" + collectionName() + "/points/scroll", body))) {
                Map<?, ?> value = asMap(point);
                Map<?, ?> payload = asMapOrNull(value.get("payload"));
                if (payload != null) {
                    payloads.put(Long.valueOf(String.valueOf(value.get("id"))), payload);
                }
            }
        }
        return payloads;
    }

    private void ensureCollection() {
        Map<?, ?> collection = getCollection();
        if (collection == null) {
            callPut("/collections/" + collectionName(), Map.of(
                    "vectors", Map.of("size", embeddingProvider.getDimension(), "distance", "Cosine")
            ));
        } else {
            validateCollection(collection);
        }

        for (String field : List.of("userId", "snapshotId", "embeddingDimension")) {
            callPut("/collections/" + collectionName() + "/index", Map.of("field_name", field, "field_schema", "integer"));
        }
        for (String field : List.of("documentType", "chunkingVersion", "contentHash", "embeddingProvider", "embeddingModel")) {
            callPut("/collections/" + collectionName() + "/index", Map.of("field_name", field, "field_schema", "keyword"));
        }
    }

    private Map<?, ?> getCollection() {
        try {
            return client.get().uri("/collections/" + collectionName()).retrieve().body(Map.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return null;
            }
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
        }
    }

    private void validateCollection(Map<?, ?> response) {
        Object result = response.get("result");
        Object config = result instanceof Map<?, ?> value ? value.get("config") : null;
        Object params = config instanceof Map<?, ?> value ? value.get("params") : null;
        Object vectors = params instanceof Map<?, ?> value ? value.get("vectors") : null;
        if (!(vectors instanceof Map<?, ?> value)
                || !Integer.valueOf(embeddingProvider.getDimension()).equals(asInteger(value.get("size")))
                || !"COSINE".equalsIgnoreCase(String.valueOf(value.get("distance")))) {
            throw new CustomException(ErrorCode.VECTOR_COLLECTION_INCOMPATIBLE);
        }
    }

    private Map<String, Object> payload(AnalysisMaterialChunk chunk) {
        return Map.of(
                "chunkId", chunk.getChunkId(),
                "userId", chunk.getUserId(),
                "snapshotId", chunk.getSnapshot().getSnapshotId(),
                "documentType", chunk.getDocumentType().name(),
                "chunkingVersion", chunk.getChunkingVersion(),
                "contentHash", chunk.getContentHash(),
                "embeddingProvider", embeddingProvider.getProviderName(),
                "embeddingModel", embeddingProvider.getModelName(),
                "embeddingDimension", embeddingProvider.getDimension()
        );
    }

    private boolean matchesPayload(Map<?, ?> actual, AnalysisMaterialChunk chunk) {
        if (actual == null) {
            return false;
        }
        return payload(chunk).entrySet().stream().allMatch(entry ->
                Objects.equals(String.valueOf(entry.getValue()), String.valueOf(actual.get(entry.getKey())))
        );
    }

    private List<?> points(Map<?, ?> response) {
        Object result = response.get("result");
        return result instanceof Map<?, ?> value && value.get("points") instanceof List<?> points ? points : List.of();
    }

    private Map<?, ?> asMap(Object value) {
        Map<?, ?> map = asMapOrNull(value);
        if (map == null) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR, "Qdrant response is invalid");
        }
        return map;
    }

    private Map<?, ?> asMapOrNull(Object value) {
        return value instanceof Map<?, ?> map ? map : null;
    }

    private Map<String, Object> equalsFilter(String key, Object value) {
        return Map.of("key", key, "match", Map.of("value", value));
    }

    private Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Map<?, ?> callPost(String uri, Object body) {
        try {
            return client.post().uri(uri).body(body).retrieve().body(Map.class);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
        }
    }

    private Map<?, ?> callPut(String uri, Object body) {
        try {
            return client.put().uri(uri).body(body).retrieve().body(Map.class);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR);
        }
    }

    private String safeToken(String value, int maxLength) {
        String normalized = value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) {
            return "value";
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String shortHash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            for (int index = 0; index < 6; index++) {
                output.append(String.format("%02x", bytes[index]));
            }
            return output.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record IndexScope(Long userId, Long snapshotId, UserDocumentType documentType) {
    }
}
