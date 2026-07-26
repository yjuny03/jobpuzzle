package com.example.jobpuzzle.analysis.rag.search;

import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchRequest;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchResult;
import com.example.jobpuzzle.analysis.rag.embedding.EmbeddingProvider;
import com.example.jobpuzzle.analysis.rag.embedding.EmbeddingResult;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Comparator;
import java.util.List;

/** 현재 MVP에서 DB 벡터 저장 없이 청크를 즉시 임베딩해 cosine 검색하는 adapter다. */
@Service
@ConditionalOnProperty(prefix = "app.rag", name = "mode", havingValue = "fake", matchIfMissing = true)
@RequiredArgsConstructor
public class InMemoryVectorSearchAdapter implements VectorSearchPort {

    private final AnalysisMaterialChunkRepository analysisMaterialChunkRepository;
    private final EmbeddingProvider embeddingProvider;

    // 현재 MVP에서는 검색 계약 검증이 목적이므로 임베딩을 메모리에서 계산한다.
    @Override
    public List<AnalysisMaterialChunkSearchResult> search(AnalysisMaterialChunkSearchRequest request) {
        validateRequest(request);
        List<AnalysisMaterialChunk> candidates = analysisMaterialChunkRepository
                .findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(
                        request.userId(), request.snapshotId(), request.allowedDocumentTypes(), request.chunkingVersion());
        if (candidates.isEmpty()) return List.of();

        EmbeddingResult queryEmbedding = embeddingProvider.embed(request.queryText());
        List<ScoredChunk> scored = candidates.stream()
                .map(chunk -> new ScoredChunk(chunk, cosineSimilarity(queryEmbedding.vector(), embeddingProvider.embed(chunk.getContent()).vector())))
                .sorted(scoreOrder())
                .limit(request.topK())
                .toList();
        return java.util.stream.IntStream.range(0, scored.size())
                .mapToObj(index -> result(scored.get(index), index + 1))
                .toList();
    }

    // 입력 검증을 검색 시작점에 모아 잘못된 격리 조건이나 topK 호출을 차단한다.
    private void validateRequest(AnalysisMaterialChunkSearchRequest request) {
        if (request == null || request.userId() == null || request.snapshotId() == null
                || request.queryText() == null || request.queryText().isBlank() || request.topK() < 1
                || request.allowedDocumentTypes() == null || request.allowedDocumentTypes().isEmpty()
                || request.chunkingVersion() == null || request.chunkingVersion().isBlank()) {
            throw new IllegalArgumentException("userId, snapshotId, queryText, topK, document types and chunking version are required");
        }
    }

    // 동점에도 snapshot source·chunk 순서가 변하지 않게 해 재현 가능한 검색 결과를 만든다.
    private Comparator<ScoredChunk> scoreOrder() {
        return Comparator.comparingDouble(ScoredChunk::score).reversed()
                .thenComparing(value -> value.chunk().getSnapshotSource().getSnapshotSourceId())
                .thenComparingInt(value -> value.chunk().getChunkIndex())
                .thenComparing(value -> value.chunk().getChunkId());
    }

    // retrieval이 검색을 재실행하지 않고도 실제 adapter provider를 재사용 계약에 반영한다.
    @Override
    public String getEmbeddingProviderName() {
        return embeddingProvider.getProviderName();
    }

    // retrieval이 검색을 재실행하지 않고도 실제 adapter model을 재사용 계약에 반영한다.
    @Override
    public String getEmbeddingModelName() {
        return embeddingProvider.getModelName();
    }

    // 저장된 청크의 원문 위치 메타데이터를 변형 없이 검색 결과 계약으로 옮긴다.
    private AnalysisMaterialChunkSearchResult result(ScoredChunk value, int rank) {
        AnalysisMaterialChunk chunk = value.chunk();
        return new AnalysisMaterialChunkSearchResult(chunk.getChunkId(), chunk.getSnapshotSource().getSnapshotSourceId(),
                chunk.getDocumentId(), chunk.getDocumentType(), chunk.getPageStart(), chunk.getPageEnd(),
                chunk.getCharStart(), chunk.getCharEnd(), chunk.getChunkIndex(), chunk.getContent(), value.score(), rank);
    }

    // cosine 입력의 길이·유한값·norm을 검증해 잘못된 provider 결과를 명확히 거부한다.
    static double cosineSimilarity(double[] left, double[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0 || left.length != right.length) {
            throw new IllegalArgumentException("cosine vectors must be non-empty and have equal dimensions");
        }
        double dot = 0.0d;
        double leftSquaredNorm = 0.0d;
        double rightSquaredNorm = 0.0d;
        for (int index = 0; index < left.length; index++) {
            if (!Double.isFinite(left[index]) || !Double.isFinite(right[index])) {
                throw new IllegalArgumentException("cosine vectors must contain only finite values");
            }
            dot += left[index] * right[index];
            leftSquaredNorm += left[index] * left[index];
            rightSquaredNorm += right[index] * right[index];
        }
        if (leftSquaredNorm == 0.0d || rightSquaredNorm == 0.0d) {
            throw new IllegalArgumentException("cosine vectors must not be zero vectors");
        }
        return dot / (Math.sqrt(leftSquaredNorm) * Math.sqrt(rightSquaredNorm));
    }

    private record ScoredChunk(AnalysisMaterialChunk chunk, double score) {
    }
}
