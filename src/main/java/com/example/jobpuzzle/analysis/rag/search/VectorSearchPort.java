package com.example.jobpuzzle.analysis.rag.search;

import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchRequest;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchResult;

import java.util.List;

/** 벡터 저장소 구현이 바뀌어도 analysis RAG 호출 계약을 유지하는 검색 포트다. */
public interface VectorSearchPort {

    // 사용자·snapshot 격리 조건 안에서 query의 상위 청크를 순위와 함께 반환한다.
    List<AnalysisMaterialChunkSearchResult> search(AnalysisMaterialChunkSearchRequest request);

    // 실제 adapter가 검색에 사용한 provider를 retrieval 재사용 계약에 고정한다.
    String getEmbeddingProviderName();

    // 실제 adapter가 검색에 사용한 model을 retrieval 재사용 계약에 고정한다.
    String getEmbeddingModelName();
}
