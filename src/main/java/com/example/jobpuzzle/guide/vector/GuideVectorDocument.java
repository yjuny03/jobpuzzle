package com.example.jobpuzzle.guide.vector;

/** 영속 엔티티를 외부 벡터 저장소에 직접 노출하지 않는 가이드 청크 인덱싱 계약이다. */
public record GuideVectorDocument(
        Long chunkId,
        Long guideId,
        String guideCode,
        String guideVersion,
        int chunkIndex,
        String title,
        String content
) {
}
