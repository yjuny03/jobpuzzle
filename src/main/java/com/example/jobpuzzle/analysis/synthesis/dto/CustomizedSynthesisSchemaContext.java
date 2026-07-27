package com.example.jobpuzzle.analysis.synthesis.dto;

import java.util.List;

/** 렌더링된 prompt 역파싱 없이 v1.3 schema enum을 생성하는 구조화 context다. */
public record CustomizedSynthesisSchemaContext(
        List<String> requirementIds,
        List<String> postingEvidenceIds,
        List<String> candidateEvidenceIds
) {
    public CustomizedSynthesisSchemaContext {
        requirementIds = requirementIds == null ? List.of() : List.copyOf(requirementIds);
        postingEvidenceIds = postingEvidenceIds == null ? List.of() : List.copyOf(postingEvidenceIds);
        candidateEvidenceIds = candidateEvidenceIds == null ? List.of() : List.copyOf(candidateEvidenceIds);
    }

    public static CustomizedSynthesisSchemaContext from(CustomizedSynthesisProviderInput input) {
        if (input == null || input.evidenceCatalog() == null) {
            throw new IllegalArgumentException("provider input and evidence catalog are required");
        }
        return new CustomizedSynthesisSchemaContext(
                input.requirementCatalog().stream().map(CustomizedSynthesisProviderInput.RequirementItem::requirementId).toList(),
                input.evidenceCatalog().evidence().stream()
                        .filter(value -> value.role() == CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING)
                        .map(CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem::evidenceId).toList(),
                input.evidenceCatalog().evidence().stream()
                        .filter(value -> value.role() == CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE)
                        .map(CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem::evidenceId).toList());
    }
}
