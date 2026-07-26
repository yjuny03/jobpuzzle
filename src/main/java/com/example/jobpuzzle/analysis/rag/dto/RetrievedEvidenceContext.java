package com.example.jobpuzzle.analysis.rag.dto;

/** prompt DTO와 재사용 방지용 fingerprint 재료를 함께 전달하는 JSON-05 내부 컨텍스트다. */
public record RetrievedEvidenceContext(RetrievedEvidenceContextDto evidence, String fingerprintMaterial) {
}
