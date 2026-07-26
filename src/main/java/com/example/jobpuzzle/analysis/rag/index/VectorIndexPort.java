package com.example.jobpuzzle.analysis.rag.index;

import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;

import java.util.Collection;

/** material chunk의 외부 vector index 수명주기를 검색 계약과 분리한다. */
public interface VectorIndexPort {

    void index(Collection<AnalysisMaterialChunk> chunks);

    void delete(Collection<AnalysisMaterialChunk> chunks);

    void requireReady(Long userId, Long snapshotId, Collection<AnalysisMaterialChunk> chunks);
}
