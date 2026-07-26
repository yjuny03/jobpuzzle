package com.example.jobpuzzle.analysis.rag.dto;

import com.example.jobpuzzle.document.entity.UserDocumentType;

/** 검색 후 JSON-05 이전 단계가 사용할 청크 원문·위치·순위 정보다. */
public record AnalysisMaterialChunkSearchResult(
        Long chunkId,
        Long snapshotSourceId,
        Long documentId,
        UserDocumentType documentType,
        int pageStart,
        int pageEnd,
        int charStart,
        int charEnd,
        int chunkIndex,
        String content,
        double score,
        int rank
) {
}
