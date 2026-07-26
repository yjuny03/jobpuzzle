package com.example.jobpuzzle.analysis.rag.embedding;

/** 임베딩 구현체를 교체해도 검색 계약을 유지하기 위한 provider 경계다. */
public interface EmbeddingProvider {

    // 검색 질의와 청크를 같은 벡터 공간으로 변환한다.
    EmbeddingResult embed(String text);

    // 실행·관측 시 어떤 임베딩 제공자를 사용했는지 식별한다.
    String getProviderName();

    // 벡터 생성 규칙의 모델 식별자를 제공한다.
    String getModelName();

    // 검색 전에 벡터 호환성을 확인할 수 있도록 차원을 제공한다.
    int getDimension();
}
