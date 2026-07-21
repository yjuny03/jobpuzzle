package com.example.jobpuzzle.document.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class DocumentDetailResponse {

    private final DocumentResponse document;
    private final ExtractionVersionResponse latestExtraction;
    private final List<ExtractionVersionResponse> confirmedVersions;

    private DocumentDetailResponse(
            DocumentResponse document,
            ExtractionVersionResponse latestExtraction,
            List<ExtractionVersionResponse> confirmedVersions
    ) {
        this.document = document;
        this.latestExtraction = latestExtraction;
        this.confirmedVersions = confirmedVersions;
    }

    public static DocumentDetailResponse of(
            DocumentResponse document,
            ExtractionVersionResponse latestExtraction,
            List<ExtractionVersionResponse> confirmedVersions
    ) {
        return new DocumentDetailResponse(document, latestExtraction, confirmedVersions);
    }
}