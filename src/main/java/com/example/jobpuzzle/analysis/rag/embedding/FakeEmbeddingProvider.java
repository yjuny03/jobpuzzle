package com.example.jobpuzzle.analysis.rag.embedding;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.nio.charset.StandardCharsets;

/** 실제 의미 품질이 아닌 검색 흐름의 결정성과 격리를 검증하는 고정 Fake provider다. */
@Component
@ConditionalOnProperty(prefix = "app.rag", name = "mode", havingValue = "fake", matchIfMissing = true)
public class FakeEmbeddingProvider implements EmbeddingProvider {

    private static final int DIMENSION = 64;
    private static final String PROVIDER_NAME = "fake";
    private static final String MODEL_NAME = "deterministic-char-ngram-v1";

    // 문자 2-gram을 고정 bucket에 누적해 비슷한 문자열이 일부 feature를 공유하게 한다.
    @Override
    public EmbeddingResult embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("embedding text must not be blank");
        }
        double[] vector = new double[DIMENSION];
        if (text.length() == 1) {
            addFeature(vector, text);
        } else {
            for (int index = 0; index < text.length() - 1; index++) {
                addFeature(vector, text.substring(index, index + 2));
            }
        }
        normalize(vector);
        return new EmbeddingResult(PROVIDER_NAME, MODEL_NAME, DIMENSION, vector);
    }

    // provider 식별자는 실제 provider 교체 시에도 검색 로그 계약을 유지한다.
    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    // 모델 식별자는 동일 provider 내의 벡터 생성 규칙을 구분한다.
    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    // 고정 dimension은 query와 candidate vector의 호환성 검증 기준이다.
    @Override
    public int getDimension() {
        return DIMENSION;
    }

    // UTF-8 기반 고정 hash로 JVM 난수 상태와 무관한 bucket과 부호를 계산한다.
    private void addFeature(double[] vector, String feature) {
        int hash = stableHash(feature);
        int bucket = Math.floorMod(hash, DIMENSION);
        vector[bucket] += (hash & 1) == 0 ? 1.0d : -1.0d;
    }

    private int stableHash(String value) {
        int hash = 0x811C9DC5;
        for (byte valueByte : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= valueByte & 0xFF;
            hash *= 0x01000193;
        }
        return hash;
    }

    // cosine 계산 가능한 unit vector를 만들고 0 vector 발생은 즉시 차단한다.
    private void normalize(double[] vector) {
        double squaredNorm = 0.0d;
        for (double value : vector) squaredNorm += value * value;
        if (squaredNorm == 0.0d) {
            throw new IllegalStateException("fake embedding produced a zero vector");
        }
        double norm = Math.sqrt(squaredNorm);
        for (int index = 0; index < vector.length; index++) vector[index] /= norm;
    }
}
