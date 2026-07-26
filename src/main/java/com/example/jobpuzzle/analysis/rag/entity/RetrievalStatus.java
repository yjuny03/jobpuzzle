package com.example.jobpuzzle.analysis.rag.entity;

public enum RetrievalStatus {
    // topK 후보 청크를 하나 이상 고정한 정상 검색 결과다.
    COMPLETED,
    // 검색 범위에 후보가 없어 chunk 없이 고정한 정상 검색 결과다.
    EMPTY
}
