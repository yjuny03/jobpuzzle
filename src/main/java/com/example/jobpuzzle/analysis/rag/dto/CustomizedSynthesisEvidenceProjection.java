package com.example.jobpuzzle.analysis.rag.dto;

import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalStatus;
import com.example.jobpuzzle.document.entity.UserDocumentType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON-05 prompt 전용 retrieval 표현이다. 동일 chunk 원문은 한 번만 보내고 requirement는 evidenceId로만 참조한다.
 * 원본 {@link RetrievedEvidenceContextDto}는 validator와 fingerprint의 권위 입력으로 그대로 유지한다.
 */
public record CustomizedSynthesisEvidenceProjection(
        List<RequirementEvidence> requirements,
        List<CandidateEvidence> candidateEvidence
) {

    public static CustomizedSynthesisEvidenceProjection from(RetrievedEvidenceContextDto source) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(source.requirements(), "source.requirements");

        Map<Long, CandidateEvidence> uniqueEvidence = new LinkedHashMap<>();
        List<RequirementEvidence> requirements = new ArrayList<>();
        for (RetrievedEvidenceContextDto.RequirementEvidence requirement : source.requirements()) {
            Objects.requireNonNull(requirement, "requirement");
            List<String> evidenceIds = new ArrayList<>();
            for (RetrievedEvidenceContextDto.RetrievedChunk chunk : Objects.requireNonNull(requirement.chunks(), "requirement.chunks")) {
                Objects.requireNonNull(chunk, "chunk");
                Long chunkId = Objects.requireNonNull(chunk.chunkId(), "chunk.chunkId");
                CandidateEvidence evidence = uniqueEvidence.computeIfAbsent(chunkId, ignored -> CandidateEvidence.from(chunk));
                evidenceIds.add(evidence.evidenceId());
            }
            requirements.add(new RequirementEvidence(requirement.requirementId(), requirement.requirementType(),
                    requirement.requirementText(), requirement.retrievalStatus(), List.copyOf(evidenceIds)));
        }
        return new CustomizedSynthesisEvidenceProjection(List.copyOf(requirements), List.copyOf(uniqueEvidence.values()));
    }

    public record RequirementEvidence(String requirementId, RequirementType requirementType, String requirementText,
                                      RetrievalStatus retrievalStatus, List<String> candidateEvidenceIds) {
    }

    public record CandidateEvidence(String evidenceId, Long chunkId, Long extractionId, Long documentId,
                                    UserDocumentType documentType, int pageStart, int pageEnd,
                                    int charStart, int charEnd, String content) {

        private static CandidateEvidence from(RetrievedEvidenceContextDto.RetrievedChunk chunk) {
            return new CandidateEvidence("chunk-" + chunk.chunkId(), chunk.chunkId(), chunk.extractionId(),
                    chunk.documentId(), chunk.documentType(), chunk.pageStart(), chunk.pageEnd(), chunk.charStart(),
                    chunk.charEnd(), chunk.content());
        }
    }
}
