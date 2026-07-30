package com.example.jobpuzzle.guide.vector;

/** 가이드 검색 결과의 청크 식별자와 유사도만 반환해 DB 원문을 신뢰 원본으로 유지한다. */
public record GuideVectorHit(Long chunkId, double score) {
}
