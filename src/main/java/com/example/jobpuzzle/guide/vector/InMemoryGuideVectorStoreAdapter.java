package com.example.jobpuzzle.guide.vector;

import com.example.jobpuzzle.analysis.rag.embedding.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** 로컬·테스트 환경에서 외부 Qdrant 없이 동일한 인덱싱/검색 계약을 검증한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rag", name = "mode", havingValue = "fake", matchIfMissing = true)
public class InMemoryGuideVectorStoreAdapter implements GuideVectorStorePort {

    private final EmbeddingProvider embeddingProvider;
    private final Map<Long, StoredVector> vectors = new ConcurrentHashMap<>();

    @Override
    public Map<Long, String> index(Long guideId, List<GuideVectorDocument> documents) {
        vectors.entrySet().removeIf(entry -> entry.getValue().guideId().equals(guideId));
        documents.forEach(document -> vectors.put(
                document.chunkId(),
                new StoredVector(guideId, embeddingProvider.embed(document.content()).vector())));
        return documents.stream().collect(Collectors.toMap(
                GuideVectorDocument::chunkId,
                document -> "memory-guide:" + document.chunkId()));
    }

    @Override
    public List<GuideVectorHit> search(Long guideId, String queryText, int topK) {
        double[] query = embeddingProvider.embed(queryText).vector();
        return vectors.entrySet().stream()
                .filter(entry -> entry.getValue().guideId().equals(guideId))
                .map(entry -> new GuideVectorHit(
                        entry.getKey(), cosine(query, entry.getValue().vector())))
                .sorted(Comparator.comparingDouble(GuideVectorHit::score).reversed()
                        .thenComparing(GuideVectorHit::chunkId))
                .limit(topK)
                .toList();
    }

    @Override public String provider() { return embeddingProvider.getProviderName(); }
    @Override public String model() { return embeddingProvider.getModelName(); }
    @Override public int dimension() { return embeddingProvider.getDimension(); }

    private double cosine(double[] left, double[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record StoredVector(Long guideId, double[] vector) {
    }
}
