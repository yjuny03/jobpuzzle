package com.example.jobpuzzle.document.dto;

import lombok.Getter;

import java.time.LocalDateTime;

// 추출 실행(202 Accepted) 응답 - 완료 결과는 GET /jobpuzzle/documents/{documentId}/extractions로 폴링해 확인한다
@Getter
public class ExtractionJobResponse {

    private static final String PROCESSING = "PROCESSING";

    private final Long documentId;
    private final Long extractionId;
    private final String status;
    private final LocalDateTime requestedAt;

    private ExtractionJobResponse(Long documentId, Long extractionId, String status, LocalDateTime requestedAt) {
        this.documentId = documentId;
        this.extractionId = extractionId;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public static ExtractionJobResponse accepted(Long documentId) {
        return new ExtractionJobResponse(documentId, null, PROCESSING, LocalDateTime.now());
    }
}
