package com.example.jobpuzzle.analysis.rag.search;

import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.analysis.rag.config.RagProperties;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchRequest;
import com.example.jobpuzzle.analysis.rag.embedding.EmbeddingProvider;
import com.example.jobpuzzle.analysis.rag.embedding.EmbeddingResult;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QdrantVectorSearchAdapterTest {

    @Test
    void usesDifferentPhysicalCollectionsForDifferentModelsOrDimensions() {
        QdrantVectorSearchAdapter modelA = fixture("text-embedding-3-small", 2).adapter();
        QdrantVectorSearchAdapter modelB = fixture("text-embedding-3-large", 2).adapter();
        QdrantVectorSearchAdapter dimensionChanged = fixture("text-embedding-3-small", 3).adapter();

        assertThat(modelA.collectionName()).isNotEqualTo(modelB.collectionName());
        assertThat(modelA.collectionName()).isNotEqualTo(dimensionChanged.collectionName());
        assertThat(modelA.collectionName()).contains("text-embedding-3-small").contains("-d2-");
    }

    @Test
    void skipsEmbeddingAndUpsertWhenTheIndexedPayloadMatches() {
        Fixture fixture = fixture("text-embedding-3-small", 2);
        expectExistingCollectionAndIndexes(fixture);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(pointResponse(payload(1L, 2L, 3L, "same-hash", "text-embedding-3-small", 2)), MediaType.APPLICATION_JSON));

        fixture.adapter().index(List.of(fixture.chunk()));

        fixture.server().verify();
        verify(fixture.embeddingProvider(), never()).embed(anyString());
    }

    @Test
    void reembedsAndUpsertsWhenContentHashChanges() {
        Fixture fixture = fixture("text-embedding-3-small", 2);
        expectExistingCollectionAndIndexes(fixture);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess(pointResponse(payload(1L, 2L, 3L, "old-hash", "text-embedding-3-small", 2)), MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(containsString("same-hash")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        fixture.adapter().index(List.of(fixture.chunk()));

        fixture.server().verify();
        verify(fixture.embeddingProvider()).embed("chunk text");
    }

    @Test
    void doesNotReusePointFromAnotherUserOrSnapshot() {
        Fixture fixture = fixture("text-embedding-3-small", 2);
        expectExistingCollectionAndIndexes(fixture);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess(pointResponse(payload(1L, 99L, 98L, "same-hash", "text-embedding-3-small", 2)), MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        fixture.adapter().index(List.of(fixture.chunk()));

        fixture.server().verify();
        verify(fixture.embeddingProvider()).embed("chunk text");
    }

    @Test
    void createsCosineCollectionAndAllPayloadIndexesWhenCollectionIsMissing() {
        Fixture fixture = fixture("text-embedding-3-small", 2);
        fixture.server().expect(requestTo(containsString("/collections/")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        fixture.server().expect(requestTo(containsString("/collections/")))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(containsString("Cosine")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        fixture.server().expect(ExpectedCount.times(8), requestTo(containsString("/index")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(content().string(containsString("contentHash")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        fixture.adapter().index(List.of(fixture.chunk()));

        fixture.server().verify();
    }

    @Test
    void sendsScopedFilteredSearch() {
        Fixture fixture = fixture("text-embedding-3-small", 2);
        when(fixture.chunkRepository().findById(1L)).thenReturn(Optional.of(fixture.chunk()));
        fixture.server().expect(requestTo(containsString("/points/query")))
                .andExpect(content().string(containsString("documentType")))
                .andExpect(content().string(containsString("chunkingVersion")))
                .andRespond(withSuccess("{\"result\":{\"points\":[{\"id\":1,\"score\":0.9,\"payload\":{}}]}}", MediaType.APPLICATION_JSON));

        fixture.adapter().search(new AnalysisMaterialChunkSearchRequest(
                2L, 3L, "query", 3, Set.of(UserDocumentType.RESUME), "v1"
        ));

        fixture.server().verify();
    }

    @Test
    void mapsQdrantFailuresToTheDedicatedErrorCode() {
        Fixture fixture = fixture("text-embedding-3-small", 2);
        fixture.server().expect(requestTo(containsString("/points/scroll"))).andRespond(withServerError());

        assertThatThrownBy(() -> fixture.adapter().requireReady(2L, 3L, List.of(fixture.chunk())))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.VECTOR_STORE_ERROR);
    }

    // --- 이하 app.rag.qdrant.capacity(용량 보호) 테스트. capacity.enabled 기본값이 false라
    // 기존 테스트(fixture())는 이 코드 경로를 전혀 타지 않는다 — 그래서 위 기존 테스트들은
    // 이번 변경으로 손댈 필요가 없었다.

    @Test
    void skipsCapacityCheckWhenCapacityGuardDisabled() {
        // capacity.enabled=false(기본값)면 chunk가 완전히 새로 생기는 point여도 /points/count를
        // 아예 호출하지 않고, upsert도 wait 파라미터 없이 지금까지와 동일하게 나간다.
        Fixture fixture = fixture("text-embedding-3-small", 2);
        expectExistingCollectionAndIndexes(fixture);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        fixture.adapter().index(List.of(fixture.chunk()));

        // /points/count에 대한 expectation을 등록하지 않았으므로, 코드가 실수로 그걸 호출하면
        // MockRestServiceServer가 "매칭되는 expectation 없음"으로 여기서 실패한다.
        fixture.server().verify();
        verify(fixture.embeddingProvider()).embed("chunk text");
    }

    @Test
    void allowsIndexingWhenProjectedCountEqualsEffectiveLimit() {
        CapacityFixture fixture = capacityFixture(10, 0, 0, true);
        expectCollectionReady(fixture.server(), 2);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":[]}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points/count")))
                .andExpect(content().string(containsString("\"exact\":true")))
                .andRespond(withSuccess(countResponse(9), MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points?wait=true")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        fixture.adapter().index(List.of(fixture.chunk()));

        fixture.server().verify();
        verify(fixture.embeddingProvider()).embed("chunk text");
    }

    @Test
    void blocksIndexingWhenProjectedCountExceedsEffectiveLimitByOne() {
        CapacityFixture fixture = capacityFixture(10, 0, 0, true);
        expectCollectionReady(fixture.server(), 2);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":[]}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points/count")))
                .andRespond(withSuccess(countResponse(10), MediaType.APPLICATION_JSON));
        // upsert(PUT .../points?wait=true)에는 expectation을 등록하지 않는다 — 차단되면
        // 절대 호출되면 안 되고, 실수로 호출되면 MockRestServiceServer가 실패시켜준다.

        assertThatThrownBy(() -> fixture.adapter().index(List.of(fixture.chunk())))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.VECTOR_CAPACITY_EXCEEDED);

        fixture.server().verify();
        verify(fixture.embeddingProvider(), never()).embed(anyString());
    }

    @Test
    void countsOnlyChunksMissingFromQdrantAsNewPoints() {
        // chunk1은 findIndexedPayloads() 스코프 조회에는 안 잡히지만(payload 스코프 불일치를
        // 흉내냄) 존재 여부 조회(/points, id 기준)에서는 이미 있다고 나온다 -> 갱신, 신규 아님.
        // chunk2는 두 조회 모두에서 안 잡힌다 -> 진짜 신규. hardLimit을 1로 두면, 신규 개수를
        // 잘못 2로 계산했을 때만 차단되므로, 정상 통과 여부로 계산이 맞았는지 간접 검증한다.
        CapacityFixture fixture = capacityFixture(1, 0, 0, true);
        AnalysisMaterialChunk chunk1 = capacityChunk(1L, "chunk one");
        AnalysisMaterialChunk chunk2 = capacityChunk(2L, "chunk two");
        expectCollectionReady(fixture.server(), 2);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(existingIdsResponse(1L), MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points/count")))
                .andRespond(withSuccess(countResponse(0), MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points?wait=true")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        fixture.adapter().index(List.of(chunk1, chunk2));

        fixture.server().verify();
        verify(fixture.embeddingProvider()).embed("chunk one");
        verify(fixture.embeddingProvider()).embed("chunk two");
    }

    @Test
    void doesNotQueryCapacityWhenOnlyUpdatingExistingPoints() {
        // 존재 여부 조회에서 이미 있다고 나오면(=갱신뿐) actualNewPointCount는 0이라
        // /points/count를 아예 호출하지 않고 upsert만 진행해야 한다.
        CapacityFixture fixture = capacityFixture(1, 0, 0, true);
        expectCollectionReady(fixture.server(), 2);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(existingIdsResponse(1L), MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points?wait=true")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        fixture.adapter().index(List.of(fixture.chunk()));

        // /points/count expectation이 없으므로, 호출됐다면 여기서 이미 실패했을 것이다.
        fixture.server().verify();
        verify(fixture.embeddingProvider()).embed("chunk text");
    }

    @Test
    void mapsPointCountFailureToVectorStoreError() {
        CapacityFixture fixture = capacityFixture(10, 0, 0, true);
        expectCollectionReady(fixture.server(), 2);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":[]}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points/count"))).andRespond(withServerError());

        assertThatThrownBy(() -> fixture.adapter().index(List.of(fixture.chunk())))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.VECTOR_STORE_ERROR);
        verify(fixture.embeddingProvider(), never()).embed(anyString());
    }

    @Test
    void mapsInvalidPointCountResponseToVectorStoreError() {
        CapacityFixture fixture = capacityFixture(10, 0, 0, true);
        expectCollectionReady(fixture.server(), 2);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":[]}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points/count")))
                .andRespond(withSuccess("{\"result\":{}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.adapter().index(List.of(fixture.chunk())))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.VECTOR_STORE_ERROR);
        verify(fixture.embeddingProvider(), never()).embed(anyString());
    }

    @Test
    void releasesLockAfterFailureSoNextRequestFromAnotherThreadSucceeds() throws InterruptedException {
        // ReentrantLock은 같은 스레드의 재진입을 허용하므로, 같은 스레드에서 재호출해 성공하는 것만으로는
        // finally에서 unlock()이 실제로 호출됐는지 증명하지 못한다(버그가 있어도 통과해버림). 서로 다른
        // 스레드로 검증해야 진짜 락 해제를 증명한다. 두 요청을 동시에 보내는 게 아니라, 첫 스레드가 완전히
        // 끝난 뒤에만 두 번째 스레드를 시작하므로 MockRestServiceServer 호출도 순차적으로만 일어난다.
        CapacityFixture fixture = capacityFixture(1, 0, 0, false);
        expectCollectionReady(fixture.server(), 2);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":[]}", MediaType.APPLICATION_JSON));
        // 1라운드: 한도를 이미 넘긴 상태로 응답해 checkCapacity()에서 예외가 나게 만든다.
        fixture.server().expect(requestTo(containsString("/points/count")))
                .andRespond(withSuccess(countResponse(5), MediaType.APPLICATION_JSON));

        expectCollectionReady(fixture.server(), 2);
        fixture.server().expect(requestTo(containsString("/points/scroll")))
                .andRespond(withSuccess("{\"result\":{\"points\":[]}}", MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(endsWith("/points")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":[]}", MediaType.APPLICATION_JSON));
        // 2라운드: 여유가 있는 상태로 응답해 정상 통과·upsert까지 이어지게 한다.
        fixture.server().expect(requestTo(containsString("/points/count")))
                .andRespond(withSuccess(countResponse(0), MediaType.APPLICATION_JSON));
        fixture.server().expect(requestTo(containsString("/points?wait=true")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        RuntimeException[] threadAFailure = new RuntimeException[1];
        Thread threadA = new Thread(() -> {
            try {
                fixture.adapter().index(List.of(fixture.chunk()));
            } catch (RuntimeException exception) {
                threadAFailure[0] = exception;
            }
        });
        threadA.start();
        threadA.join(5_000);
        assertThat(threadA.isAlive()).isFalse();
        assertThat(threadAFailure[0]).isInstanceOf(CustomException.class);

        RuntimeException[] threadBFailure = new RuntimeException[1];
        Thread threadB = new Thread(() -> {
            try {
                fixture.adapter().index(List.of(fixture.chunk()));
            } catch (RuntimeException exception) {
                threadBFailure[0] = exception;
            }
        });
        threadB.start();
        threadB.join(5_000);

        assertThat(threadB.isAlive()).isFalse(); // 살아있다면 락이 안 풀려서 계속 대기 중이라는 뜻
        assertThat(threadBFailure[0]).isNull();
        fixture.server().verify();
        verify(fixture.embeddingProvider()).embed("chunk text");
    }

    private void expectCollectionReady(MockRestServiceServer server, int dimension) {
        server.expect(requestTo(containsString("/collections/")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(collectionResponse(dimension), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.times(8), requestTo(containsString("/index")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    }

    private void expectExistingCollectionAndIndexes(Fixture fixture) {
        fixture.server().expect(requestTo(containsString("/collections/")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(collectionResponse(fixture.embeddingProvider().getDimension()), MediaType.APPLICATION_JSON));
        fixture.server().expect(ExpectedCount.times(8), requestTo(containsString("/index")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    }

    private Fixture fixture(String model, int dimension) {
        RagProperties properties = new RagProperties();
        properties.getQdrant().setUrl("https://qdrant.test");
        properties.getQdrant().setCollection("analysis-material-chunks");
        properties.getEmbedding().setProvider("openai");
        properties.getEmbedding().setModel(model);
        properties.getEmbedding().setDimension(dimension);

        RestClient.Builder builder = RestClient.builder().baseUrl("https://qdrant.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
        when(embeddingProvider.getProviderName()).thenReturn("openai");
        when(embeddingProvider.getModelName()).thenReturn(model);
        when(embeddingProvider.getDimension()).thenReturn(dimension);
        when(embeddingProvider.embed("chunk text")).thenReturn(new EmbeddingResult("openai", model, dimension, vector(dimension)));
        when(embeddingProvider.embed("query")).thenReturn(new EmbeddingResult("openai", model, dimension, vector(dimension)));

        AnalysisMaterialChunkRepository chunkRepository = mock(AnalysisMaterialChunkRepository.class);
        AnalysisMaterialChunk chunk = mock(AnalysisMaterialChunk.class, RETURNS_DEEP_STUBS);
        when(chunk.getChunkId()).thenReturn(1L);
        when(chunk.getUserId()).thenReturn(2L);
        when(chunk.getSnapshot().getSnapshotId()).thenReturn(3L);
        when(chunk.getSnapshotSource().getSnapshotSourceId()).thenReturn(4L);
        when(chunk.getDocumentId()).thenReturn(5L);
        when(chunk.getDocumentType()).thenReturn(UserDocumentType.RESUME);
        when(chunk.getChunkingVersion()).thenReturn("v1");
        when(chunk.getContentHash()).thenReturn("same-hash");
        when(chunk.getContent()).thenReturn("chunk text");

        return new Fixture(
                new QdrantVectorSearchAdapter(properties, embeddingProvider, chunkRepository, builder.build()),
                server,
                embeddingProvider,
                chunkRepository,
                chunk
        );
    }

    private double[] vector(int dimension) {
        double[] vector = new double[dimension];
        for (int index = 0; index < dimension; index++) {
            vector[index] = index + 0.1;
        }
        return vector;
    }

    private String collectionResponse(int dimension) {
        return "{\"result\":{\"config\":{\"params\":{\"vectors\":{\"size\":" + dimension + ",\"distance\":\"Cosine\"}}}}}";
    }

    private String pointResponse(String payload) {
        return "{\"result\":{\"points\":[{\"id\":1,\"payload\":" + payload + "}]}}";
    }

    private String payload(Long chunkId, Long userId, Long snapshotId, String contentHash, String model, int dimension) {
        return "{\"chunkId\":" + chunkId + ",\"userId\":" + userId + ",\"snapshotId\":" + snapshotId
                + ",\"documentType\":\"RESUME\",\"chunkingVersion\":\"v1\",\"contentHash\":\"" + contentHash
                + "\",\"embeddingProvider\":\"openai\",\"embeddingModel\":\"" + model
                + "\",\"embeddingDimension\":" + dimension + "}";
    }

    private record Fixture(
            QdrantVectorSearchAdapter adapter,
            MockRestServiceServer server,
            EmbeddingProvider embeddingProvider,
            AnalysisMaterialChunkRepository chunkRepository,
            AnalysisMaterialChunk chunk
    ) {
    }

    // --- capacity(용량 보호) 전용 fixture. 기존 fixture()/Fixture는 그대로 두고 완전히 별도로 둔다.

    private CapacityFixture capacityFixture(long hardLimit, long safetyMargin, long warningThreshold, boolean ignoreExpectOrder) {
        RagProperties properties = new RagProperties();
        properties.getQdrant().setUrl("https://qdrant.test");
        properties.getQdrant().setCollection("analysis-material-chunks");
        properties.getEmbedding().setProvider("openai");
        properties.getEmbedding().setModel("text-embedding-3-small");
        properties.getEmbedding().setDimension(2);
        properties.getQdrant().getCapacity().setEnabled(true);
        properties.getQdrant().getCapacity().setHardLimit(hardLimit);
        properties.getQdrant().getCapacity().setSafetyMargin(safetyMargin);
        properties.getQdrant().getCapacity().setWarningThreshold(warningThreshold);

        RestClient.Builder builder = RestClient.builder().baseUrl("https://qdrant.test");
        MockRestServiceServer.MockRestServiceServerBuilder serverBuilder = MockRestServiceServer.bindTo(builder);
        if (ignoreExpectOrder) {
            serverBuilder.ignoreExpectOrder(true);
        }
        MockRestServiceServer server = serverBuilder.build();

        EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
        when(embeddingProvider.getProviderName()).thenReturn("openai");
        when(embeddingProvider.getModelName()).thenReturn("text-embedding-3-small");
        when(embeddingProvider.getDimension()).thenReturn(2);
        when(embeddingProvider.embed(anyString()))
                .thenReturn(new EmbeddingResult("openai", "text-embedding-3-small", 2, vector(2)));

        AnalysisMaterialChunkRepository chunkRepository = mock(AnalysisMaterialChunkRepository.class);
        AnalysisMaterialChunk chunk = capacityChunk(1L, "chunk text");

        return new CapacityFixture(
                new QdrantVectorSearchAdapter(properties, embeddingProvider, chunkRepository, builder.build()),
                server, embeddingProvider, chunk);
    }

    private AnalysisMaterialChunk capacityChunk(long chunkId, String content) {
        AnalysisMaterialChunk chunk = mock(AnalysisMaterialChunk.class, RETURNS_DEEP_STUBS);
        when(chunk.getChunkId()).thenReturn(chunkId);
        when(chunk.getUserId()).thenReturn(2L);
        when(chunk.getSnapshot().getSnapshotId()).thenReturn(3L);
        when(chunk.getSnapshotSource().getSnapshotSourceId()).thenReturn(4L);
        when(chunk.getDocumentId()).thenReturn(5L);
        when(chunk.getDocumentType()).thenReturn(UserDocumentType.RESUME);
        when(chunk.getChunkingVersion()).thenReturn("v1");
        when(chunk.getContentHash()).thenReturn("hash-" + chunkId);
        when(chunk.getContent()).thenReturn(content);
        return chunk;
    }

    private String countResponse(long count) {
        return "{\"result\":{\"count\":" + count + "}}";
    }

    // Qdrant의 "id로 point 조회"(POST .../points) 응답은 /points/scroll과 달리
    // result 자체가 point 배열이다(result.points로 감싸지 않음).
    private String existingIdsResponse(Long... ids) {
        StringBuilder body = new StringBuilder("{\"result\":[");
        for (int index = 0; index < ids.length; index++) {
            if (index > 0) body.append(",");
            body.append("{\"id\":").append(ids[index]).append("}");
        }
        return body.append("]}").toString();
    }

    private record CapacityFixture(
            QdrantVectorSearchAdapter adapter,
            MockRestServiceServer server,
            EmbeddingProvider embeddingProvider,
            AnalysisMaterialChunk chunk
    ) {
    }
}
