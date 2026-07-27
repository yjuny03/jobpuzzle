package com.example.jobpuzzle.analysis.synthesis.dto;

import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.analysis.entity.RequirementType;

import java.util.List;

/** JSON-05 v1.3 서버 내부 전용 권위 catalog다. provider에 직접 직렬화하지 않는다. */
public record CustomizedSynthesisEvidenceCatalog(
        List<RequirementItem> requirements,
        List<EvidenceItem> evidence
) {
    public CustomizedSynthesisEvidenceCatalog {
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public record RequirementItem(
            String requirementId,
            RequirementType requirementType,
            String requirementText,
            List<String> postingEvidenceIds,
            List<String> allowedCandidateEvidenceIds
    ) {
        public RequirementItem {
            postingEvidenceIds = postingEvidenceIds == null ? List.of() : List.copyOf(postingEvidenceIds);
            allowedCandidateEvidenceIds = allowedCandidateEvidenceIds == null
                    ? List.of() : List.copyOf(allowedCandidateEvidenceIds);
        }
    }

    public record EvidenceItem(
            String evidenceId,
            EvidenceRole role,
            SourceReference sourceReference
    ) {
    }

    public enum EvidenceRole {
        POSTING,
        CANDIDATE
    }
}
