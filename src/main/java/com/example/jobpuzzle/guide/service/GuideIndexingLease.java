package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.guide.vector.GuideVectorDocument;

import java.util.List;

/** DB 잠금 해제 후 외부 벡터 저장소에 전달할 불변 인덱싱 입력이다. */
public record GuideIndexingLease(Long guideId, List<GuideVectorDocument> documents) {
}
