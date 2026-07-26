package com.example.jobpuzzle.analysis.rag.dto;

import com.example.jobpuzzle.document.entity.UserDocumentType;

import java.util.Set;

/** snapshot으로 격리된 사용자 자료 검색 입력이다. */
public record AnalysisMaterialChunkSearchRequest(
        Long userId,
        Long snapshotId,
        String queryText,
        int topK,
        Set<UserDocumentType> allowedDocumentTypes,
        String chunkingVersion
) {
}
