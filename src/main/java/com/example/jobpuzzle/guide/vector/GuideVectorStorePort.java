package com.example.jobpuzzle.guide.vector;

import java.util.List;
import java.util.Map;

/** fake/실제 벡터 저장소가 공통으로 구현하는 가이드 전용 포트다. */
public interface GuideVectorStorePort {
    Map<Long, String> index(Long guideId, List<GuideVectorDocument> documents);
    List<GuideVectorHit> search(Long guideId, String queryText, int topK);
    String provider();
    String model();
    int dimension();
}
