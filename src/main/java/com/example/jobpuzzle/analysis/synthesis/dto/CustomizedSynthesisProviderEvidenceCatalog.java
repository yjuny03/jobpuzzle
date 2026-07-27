package com.example.jobpuzzle.analysis.synthesis.dto;

import java.util.List;

/** Provider에 공개할 최소 evidence catalog다. 서버 source identity를 포함하지 않는다. */
public record CustomizedSynthesisProviderEvidenceCatalog(List<EvidenceItem> evidence) {

    public CustomizedSynthesisProviderEvidenceCatalog {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public record EvidenceItem(
            String evidenceId,
            CustomizedSynthesisEvidenceCatalog.EvidenceRole role,
            String evidenceText
    ) {
    }
}
