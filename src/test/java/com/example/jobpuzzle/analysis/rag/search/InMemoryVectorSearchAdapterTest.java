package com.example.jobpuzzle.analysis.rag.search;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshotSource;
import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchRequest;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchResult;
import com.example.jobpuzzle.analysis.rag.embedding.EmbeddingProvider;
import com.example.jobpuzzle.analysis.rag.embedding.EmbeddingResult;
import com.example.jobpuzzle.analysis.rag.embedding.FakeEmbeddingProvider;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemoryVectorSearchAdapterTest {

    private static final long USER_ID = 10L;
    private static final long SNAPSHOT_ID = 20L;

    @Mock private AnalysisMaterialChunkRepository chunkRepository;
    private InMemoryVectorSearchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new InMemoryVectorSearchAdapter(chunkRepository, new FakeEmbeddingProvider());
    }

    @Test
    void queriesRepositoryWithUserAndSnapshotIsolation() {
        AnalysisMaterialChunk candidate = chunk(1L, 1L, 0, "Spring Boot API");
        when(chunkRepository.findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(USER_ID, SNAPSHOT_ID, candidateTypes(), "v1"))
                .thenReturn(List.of(candidate));

        adapter.search(request("Spring Boot"));

        verify(chunkRepository).findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(USER_ID, SNAPSHOT_ID, candidateTypes(), "v1");
        verify(chunkRepository, never()).findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(99L, SNAPSHOT_ID, candidateTypes(), "v1");
    }

    @Test
    void returnsOnlyRepositoryCandidatesForRequestedUserAndSnapshot() {
        AnalysisMaterialChunk own = chunk(1L, 1L, 0, "Spring Boot API");
        when(chunkRepository.findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(USER_ID, SNAPSHOT_ID, candidateTypes(), "v1"))
                .thenReturn(List.of(own));

        List<AnalysisMaterialChunkSearchResult> results = adapter.search(request("Spring Boot"));

        assertThat(results).extracting(AnalysisMaterialChunkSearchResult::chunkId).containsExactly(1L);
        verify(chunkRepository, never()).findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(anyLong(), eq(99L), anySet(), eq("v1"));
    }

    @Test
    void limitsResultsToTopKAndOrdersScoresDescending() {
        List<AnalysisMaterialChunk> candidates = List.of(chunk(1L, 1L, 0, "Spring Boot API"),
                chunk(2L, 2L, 0, "Spring Boot 개발"), chunk(3L, 3L, 0, "브랜드 디자인"));
        when(chunkRepository.findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(USER_ID, SNAPSHOT_ID, candidateTypes(), "v1"))
                .thenReturn(candidates);

        List<AnalysisMaterialChunkSearchResult> results = adapter.search(new AnalysisMaterialChunkSearchRequest(USER_ID, SNAPSHOT_ID, "Spring Boot API", 2, candidateTypes(), "v1"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).score()).isGreaterThanOrEqualTo(results.get(1).score());
        assertThat(results).extracting(AnalysisMaterialChunkSearchResult::rank).containsExactly(1, 2);
    }

    @Test
    void usesStableTieOrderBySourceIndexAndChunkId() {
        List<AnalysisMaterialChunk> candidates = List.of(chunk(30L, 2L, 1, "동일 원문"),
                chunk(20L, 1L, 5, "동일 원문"), chunk(10L, 1L, 1, "동일 원문"));
        when(chunkRepository.findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(USER_ID, SNAPSHOT_ID, candidateTypes(), "v1"))
                .thenReturn(candidates);

        List<AnalysisMaterialChunkSearchResult> results = adapter.search(request("동일 원문"));

        assertThat(results).extracting(AnalysisMaterialChunkSearchResult::chunkId).containsExactly(10L, 20L, 30L);
    }

    @Test
    void returnsEmptyListWhenNoCandidateExists() {
        when(chunkRepository.findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(USER_ID, SNAPSHOT_ID, candidateTypes(), "v1"))
                .thenReturn(List.of());

        assertThat(adapter.search(request("검색"))).isEmpty();
    }

    @Test
    void rejectsBlankQueryAndInvalidTopK() {
        assertThatThrownBy(() -> adapter.search(new AnalysisMaterialChunkSearchRequest(USER_ID, SNAPSHOT_ID, " ", 1, candidateTypes(), "v1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.search(new AnalysisMaterialChunkSearchRequest(USER_ID, SNAPSHOT_ID, "검색", 0, candidateTypes(), "v1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsSameResultForSameInput() {
        List<AnalysisMaterialChunk> candidates = List.of(chunk(1L, 1L, 0, "Java Spring"), chunk(2L, 2L, 0, "React"));
        when(chunkRepository.findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(USER_ID, SNAPSHOT_ID, candidateTypes(), "v1"))
                .thenReturn(candidates);

        assertThat(adapter.search(request("Java"))).isEqualTo(adapter.search(request("Java")));
    }

    @Test
    void copiesChunkMetadataAndOriginalLocationIntoResult() {
        AnalysisMaterialChunk candidate = chunk(1L, 7L, 3, "원문 내용");
        when(chunkRepository.findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(USER_ID, SNAPSHOT_ID, candidateTypes(), "v1"))
                .thenReturn(List.of(candidate));

        AnalysisMaterialChunkSearchResult result = adapter.search(request("원문")).get(0);

        assertThat(result.snapshotSourceId()).isEqualTo(7L);
        assertThat(result.documentId()).isEqualTo(101L);
        assertThat(result.documentType()).isEqualTo(UserDocumentType.RESUME);
        assertThat(result.pageStart()).isEqualTo(2);
        assertThat(result.pageEnd()).isEqualTo(2);
        assertThat(result.charStart()).isEqualTo(11);
        assertThat(result.charEnd()).isEqualTo(15);
        assertThat(result.chunkIndex()).isEqualTo(3);
        assertThat(result.content()).isEqualTo("원문 내용");
    }

    @Test
    void rejectsDimensionMismatchAndZeroVectorInCosineCalculation() {
        assertThatThrownBy(() -> InMemoryVectorSearchAdapter.cosineSimilarity(new double[]{1.0d}, new double[]{1.0d, 0.0d}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InMemoryVectorSearchAdapter.cosineSimilarity(new double[]{0.0d}, new double[]{1.0d}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsProviderVectorsWithDifferentDimensions() {
        EmbeddingProvider mismatchedProvider = new EmbeddingProvider() {
            private int calls;
            @Override public EmbeddingResult embed(String text) { return ++calls == 1
                    ? new EmbeddingResult("fake", "mismatch", 2, new double[]{1.0d, 0.0d})
                    : new EmbeddingResult("fake", "mismatch", 3, new double[]{1.0d, 0.0d, 0.0d}); }
            @Override public String getProviderName() { return "fake"; }
            @Override public String getModelName() { return "mismatch"; }
            @Override public int getDimension() { return 2; }
        };
        adapter = new InMemoryVectorSearchAdapter(chunkRepository, mismatchedProvider);
        AnalysisMaterialChunk candidate = chunk(1L, 1L, 0, "내용");
        when(chunkRepository.findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(USER_ID, SNAPSHOT_ID, candidateTypes(), "v1"))
                .thenReturn(List.of(candidate));

        assertThatThrownBy(() -> adapter.search(request("질의"))).isInstanceOf(IllegalArgumentException.class);
    }

    private AnalysisMaterialChunkSearchRequest request(String query) {
        return new AnalysisMaterialChunkSearchRequest(USER_ID, SNAPSHOT_ID, query, 10, candidateTypes(), "v1");
    }

    private Set<UserDocumentType> candidateTypes() {
        return EnumSet.of(UserDocumentType.RESUME, UserDocumentType.COVER_LETTER,
                UserDocumentType.PORTFOLIO, UserDocumentType.EXPERIENCE_NOTE);
    }

    private AnalysisMaterialChunk chunk(Long chunkId, Long sourceId, int chunkIndex, String content) {
        AnalysisMaterialChunk chunk = mock(AnalysisMaterialChunk.class);
        AnalysisInputSnapshotSource source = mock(AnalysisInputSnapshotSource.class);
        // topK·예외 경로에서는 일부 위치 메타데이터를 읽지 않으므로 fixture stubbing만 lenient로 둔다.
        lenient().when(source.getSnapshotSourceId()).thenReturn(sourceId);
        lenient().when(chunk.getChunkId()).thenReturn(chunkId);
        lenient().when(chunk.getSnapshotSource()).thenReturn(source);
        lenient().when(chunk.getDocumentId()).thenReturn(101L);
        lenient().when(chunk.getDocumentType()).thenReturn(UserDocumentType.RESUME);
        lenient().when(chunk.getPageStart()).thenReturn(2);
        lenient().when(chunk.getPageEnd()).thenReturn(2);
        lenient().when(chunk.getCharStart()).thenReturn(11);
        lenient().when(chunk.getCharEnd()).thenReturn(15);
        lenient().when(chunk.getChunkIndex()).thenReturn(chunkIndex);
        when(chunk.getContent()).thenReturn(content);
        return chunk;
    }
}
