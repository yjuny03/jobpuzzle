package com.example.jobpuzzle.analysis.rag.embedding;

import java.util.Arrays;

/** 외부 API DTO와 분리된 내부 RAG 임베딩 결과다. */
public record EmbeddingResult(String provider, String model, int dimension, double[] vector) {

    // 유효한 고정 차원 벡터만 허용하고 배열을 복사해 결과 불변성을 보장한다.
    public EmbeddingResult {
        if (provider == null || provider.isBlank() || model == null || model.isBlank()) {
            throw new IllegalArgumentException("embedding provider and model are required");
        }
        if (dimension <= 0 || vector == null || vector.length == 0 || vector.length != dimension) {
            throw new IllegalArgumentException("embedding dimension and vector length must match");
        }
        if (Arrays.stream(vector).anyMatch(value -> Double.isNaN(value) || Double.isInfinite(value))) {
            throw new IllegalArgumentException("embedding vector must contain only finite values");
        }
        vector = vector.clone();
    }

    // 내부 배열이 호출자에게 변경되지 않도록 방어적 복사본을 반환한다.
    @Override
    public double[] vector() {
        return vector.clone();
    }
}
