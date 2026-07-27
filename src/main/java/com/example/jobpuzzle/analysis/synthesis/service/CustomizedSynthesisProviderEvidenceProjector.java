package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderEvidenceCatalog;
import org.springframework.stereotype.Component;

/** 서버 권위 catalog에서 source identity를 제거한 provider 공개 projection을 만든다. */
@Component
public class CustomizedSynthesisProviderEvidenceProjector {

    public CustomizedSynthesisProviderEvidenceCatalog project(CustomizedSynthesisEvidenceCatalog authority) {
        if (authority == null) throw new IllegalArgumentException("authority evidence catalog is required");
        return new CustomizedSynthesisProviderEvidenceCatalog(authority.evidence().stream()
                .map(value -> new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        value.evidenceId(), value.role(), value.sourceReference().getEvidenceText()))
                .toList());
    }
}
