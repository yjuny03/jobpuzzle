package com.example.jobpuzzle.analysis.rag.dto;

import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalStatus;
import com.example.jobpuzzle.document.entity.UserDocumentType;

import java.util.List;

/** JSON-05 prompt에 노출할 requirement별로 고정된 candidate 검색 근거다. */
public record RetrievedEvidenceContextDto(List<RequirementEvidence> requirements) {

    /** requirement와 그 requirement에만 허용된 선택 청크를 불변 입력으로 구성한다. */
    public record RequirementEvidence(String requirementId, RequirementType requirementType, String requirementText,
                                      RetrievalStatus retrievalStatus, List<RetrievedChunk> chunks) {
    }

    /** 선택 material chunk의 원문 위치와 본문만 JSON-05에 전달한다. */
    public record RetrievedChunk(Long chunkId, Long extractionId, Long documentId, UserDocumentType documentType,
                                 int pageStart, int pageEnd, int charStart, int charEnd, int rank,
                                 double similarityScore, String content) {
    }
}
