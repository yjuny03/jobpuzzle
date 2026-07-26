package com.example.jobpuzzle.analysis.rag.embedding;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeEmbeddingProviderTest {

    private final FakeEmbeddingProvider provider = new FakeEmbeddingProvider();

    @Test
    void returnsSameVectorForSameInput() {
        assertThat(provider.embed("Spring Boot API").vector()).containsExactly(provider.embed("Spring Boot API").vector());
    }

    @Test
    void isIndependentOfJvmRandomState() {
        new Random(1234L).nextLong();
        double[] first = provider.embed("결정적 임베딩").vector();
        new Random(9876L).nextLong();

        assertThat(provider.embed("결정적 임베딩").vector()).containsExactly(first);
    }

    @Test
    void returnsDifferentVectorForDifferentInput() {
        assertThat(provider.embed("백엔드 개발").vector()).isNotEqualTo(provider.embed("브랜드 디자인").vector());
    }

    @Test
    void returnsFiniteVectorMatchingDeclaredDimension() {
        EmbeddingResult result = provider.embed("포트폴리오 프로젝트");

        assertThat(result.dimension()).isEqualTo(provider.getDimension());
        assertThat(result.vector()).hasSize(result.dimension());
        assertThat(Arrays.stream(result.vector()).allMatch(Double::isFinite)).isTrue();
    }

    @Test
    void returnsNonZeroVector() {
        assertThat(Arrays.stream(provider.embed("한 글자").vector()).map(value -> value * value).sum()).isGreaterThan(0.0d);
    }

    @Test
    void rejectsBlankInput() {
        assertThatThrownBy(() -> provider.embed("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void embeddingResultDefensivelyCopiesVector() {
        double[] original = {1.0d, 0.0d};
        EmbeddingResult result = new EmbeddingResult("fake", "test", 2, original);
        original[0] = 7.0d;
        double[] read = result.vector();
        read[0] = 9.0d;

        assertThat(result.vector()).containsExactly(1.0d, 0.0d);
    }
}
