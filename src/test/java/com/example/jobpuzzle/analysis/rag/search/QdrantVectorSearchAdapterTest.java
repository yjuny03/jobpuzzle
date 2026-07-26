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
}
