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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/** Qdrant point의 payload filter를 DB retrieval 범위와 동일하게 강제한다. */
@Service
@ConditionalOnProperty(prefix = "app.rag", name = "mode", havingValue = "real")
public class QdrantVectorSearchAdapter implements VectorSearchPort, VectorIndexPort {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorSearchAdapter.class);

    private final RagProperties properties;
    private final EmbeddingProvider embeddingProvider;
    private final AnalysisMaterialChunkRepository chunkRepository;
    private final RestClient client;
    // capacity.enabled=true일 때만 index() 전체(count 조회~upsert)를 직렬화하는 용도로 쓴다.
    // JVM 프로세스 내부에서만 유효한 락이다 — 자세한 이유는 index() 메서드의 주석 참고.
    private final ReentrantLock capacityLock = new ReentrantLock();

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
        // capacity.enabled=false(기본값)이면 락도 /points/count 조회도 전혀 타지 않고
        // 지금까지와 완전히 동일하게 동작한다.
        if (!properties.getQdrant().getCapacity().isEnabled()) {
            indexInternal(chunks, false);
            return;
        }
        // 단일 인스턴스 전제: count 조회~판정~embed~upsert 전체를 하나의 락으로 직렬화해야
        // "두 요청이 같은 현재 point 수를 보고 동시에 통과"하는 경합을 막을 수 있다.
        // 주의: 이 락은 JVM 프로세스 내부에서만 유효하다. 다중 인스턴스로 전환하면 서로 다른
        // 프로세스의 락은 서로를 인지하지 못해 용량 보호가 무력화된다 — 그때는 MariaDB
        // 비관적 락 + 예약 point 카운터(이 프로젝트가 이미 쓰는 findWithLock* 패턴과 동일 계열)로
        // 교체해야 한다.
        capacityLock.lock();
        try {
            indexInternal(chunks, true);
        } finally {
            capacityLock.unlock();
        }
    }

    // capacityGuardActive 여부와 무관하게 chunk 필터링·upsert 순서는 동일하게 유지한다.
    private void indexInternal(Collection<AnalysisMaterialChunk> chunks, boolean capacityGuardActive) {
        ensureCollection();

        // 한 scope의 chunk ID를 한 번에 조회한다. payload 계약이 모두 같은 point만 재사용한다.
        Map<Long, Map<?, ?>> indexedPayloads = findIndexedPayloads(chunks);
        List<AnalysisMaterialChunk> chunksToUpsert = chunks.stream()
                .filter(chunk -> !matchesPayload(indexedPayloads.get(chunk.getChunkId()), chunk))
                .toList();
        if (chunksToUpsert.isEmpty()) {
            return;
        }

        if (capacityGuardActive) {
            // findIndexedPayloads()는 (userId, snapshotId, documentType) 스코프로 조회하므로,
            // 같은 point ID라도 payload의 스코프 값이 현재 DB와 다르면(예: 과거 다른 DB
            // 인스턴스로 색인된 흔적 — 실제로 이런 point가 있었다) 여기서는 안 잡힌다. 그 경우를
            // "신규"로 잘못 세면 실제로는 point 수가 늘지 않는데도 초과로 오판해 정상 요청을
            // 차단할 수 있다. 그래서 point 총량 증가 여부는 스코프 없이 ID 존재만 다시 확인한다.
            // findIndexedPayloads()는 재업로드 필요 여부 판정용으로 그대로 두고 건드리지 않는다.
            Set<Long> existingPointIds = findExistingPointIds(
                    chunksToUpsert.stream().map(AnalysisMaterialChunk::getChunkId).toList());
            long actualNewPointCount = chunksToUpsert.stream()
                    .map(AnalysisMaterialChunk::getChunkId)
                    .filter(chunkId -> !existingPointIds.contains(chunkId))
                    .count();
            if (actualNewPointCount > 0) {
                checkCapacity(actualNewPointCount);
            }
            // actualNewPointCount == 0(기존 point 갱신뿐)이면 point 총량이 늘지 않으므로
            // count 조회·용량 판정 없이 그대로 진행한다.
        }

        List<Map<String, Object>> pointsToUpsert = chunksToUpsert.stream()
                .map(chunk -> Map.of(
                        "id", chunk.getChunkId(),
                        "vector", embeddingProvider.embed(chunk.getContent()).vector(),
                        "payload", payload(chunk)
                ))
                .toList();

        // 용량 판정 후 실제 point 반영 전에 락을 해제하면, 다음 요청이 이전 upsert를 반영하지
        // 않은 count를 읽을 수 있다. 따라서 capacity guard 활성화 시에만 upsert가 실제로
        // 적용되고 조회 가능해질 때까지 기다린 뒤(wait=true) 응답받는다. capacity.enabled=false에서는
        // 이 race 자체가 무의미하므로 기존 동작(파라미터 없음)을 그대로 보존한다.
        String uri = "/collections/" + collectionName() + "/points" + (capacityGuardActive ? "?wait=true" : "");
        callPut(uri, Map.of("points", pointsToUpsert));
    }

    private void checkCapacity(long actualNewPointCount) {
        RagProperties.Qdrant.Capacity capacity = properties.getQdrant().getCapacity();
        long currentPointCount = currentPointCount();
        long effectiveLimit = capacity.getHardLimit() - capacity.getSafetyMargin();

        // currentPointCount + actualNewPointCount를 바로 더하면 비정상적인 응답값에서 long
        // overflow가 날 수 있다. 뺄셈 비교로 바꾸면 별도 overflow 처리 없이 같은 결과를 안전하게 얻는다.
        boolean exceedsCapacity = currentPointCount > effectiveLimit
                || actualNewPointCount > effectiveLimit - currentPointCount;
        if (exceedsCapacity) {
            log.warn("Qdrant capacity guard blocked indexing: currentPointCount={} actualNewPointCount={} "
                            + "hardLimit={} safetyMargin={} effectiveLimit={}",
                    currentPointCount, actualNewPointCount,
                    capacity.getHardLimit(), capacity.getSafetyMargin(), effectiveLimit);
            throw new CustomException(ErrorCode.VECTOR_CAPACITY_EXCEEDED);
        }

        // 여기까지 왔으면 합계가 effectiveLimit 이하로 확인됐으므로 overflow 없이 더할 수 있다.
        long projectedPointCount = currentPointCount + actualNewPointCount;
        long warningThreshold = capacity.getWarningThreshold();
        if (warningThreshold > 0 && projectedPointCount >= warningThreshold) {
            log.warn("Qdrant capacity guard allowed indexing near warning threshold: currentPointCount={} "
                            + "actualNewPointCount={} projectedPointCount={} warningThreshold={} effectiveLimit={}",
                    currentPointCount, actualNewPointCount, projectedPointCount, warningThreshold, effectiveLimit);
        } else {
            log.info("Qdrant capacity guard allowed indexing: currentPointCount={} actualNewPointCount={} "
                            + "projectedPointCount={} effectiveLimit={}",
                    currentPointCount, actualNewPointCount, projectedPointCount, effectiveLimit);
        }
    }

    // 순수 point ID 존재 여부만 확인한다(payload·vector 불필요). findIndexedPayloads()와 달리
    // scope 필터를 두지 않는다 — chunkId는 DB auto-increment PK라 재사용되지 않으므로, scope와
    // 무관하게 "이 ID가 이미 Qdrant point로 존재하는가"만이 point 총량 증가 여부를 정확히 답한다.
    private Set<Long> findExistingPointIds(Collection<Long> chunkIds) {
        if (chunkIds.isEmpty()) {
            return Set.of();
        }
        Map<?, ?> response = callPost("/collections/" + collectionName() + "/points", Map.of(
                "ids", chunkIds.stream().distinct().toList(),
                "with_payload", false,
                "with_vector", false
        ));
        Object result = response.get("result");
        if (!(result instanceof List<?> points)) {
            throw invalidPointResponse();
        }
        Set<Long> ids = new HashSet<>();
        try {
            for (Object point : points) {
                Object id = asMap(point).get("id");
                if (id == null) {
                    throw new IllegalArgumentException("missing point id");
                }
                ids.add(Long.parseLong(String.valueOf(id)));
            }
        } catch (RuntimeException exception) {
            throw invalidPointResponse();
        }
        return ids;
    }

    private CustomException invalidPointResponse() {
        return new CustomException(ErrorCode.VECTOR_STORE_ERROR, "Qdrant points response is invalid");
    }

    private long currentPointCount() {
        Map<?, ?> response = callPost("/collections/" + collectionName() + "/points/count", Map.of("exact", true));
        Object result = response.get("result");
        Object countValue = result instanceof Map<?, ?> value ? value.get("count") : null;
        if (!(countValue instanceof Number number)) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR, "Qdrant point count response is invalid");
        }
        long count = number.longValue();
        if (count < 0) {
            throw new CustomException(ErrorCode.VECTOR_STORE_ERROR, "Qdrant point count response is invalid");
        }
        return count;
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
