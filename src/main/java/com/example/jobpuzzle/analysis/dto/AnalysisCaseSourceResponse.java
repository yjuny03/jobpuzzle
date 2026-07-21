package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.analysis.entity.AnalysisCaseSource;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.Getter;

@Getter
public class AnalysisCaseSourceResponse {

    private final Long analysisCaseSourceId;
    private final Long extractionId;
    private final Long documentId;
    private final UserDocumentType documentType;
    private final String displayName;
    private final Integer majorVersion;
    private final Integer minorVersion;

    private AnalysisCaseSourceResponse(
            Long analysisCaseSourceId, Long extractionId, Long documentId, UserDocumentType documentType,
            String displayName, Integer majorVersion, Integer minorVersion
    ) {
        this.analysisCaseSourceId = analysisCaseSourceId;
        this.extractionId = extractionId;
        this.documentId = documentId;
        this.documentType = documentType;
        this.displayName = displayName;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public static AnalysisCaseSourceResponse from(AnalysisCaseSource source) {
        var extraction = source.getExtraction();
        var document = extraction.getDocument();
        return new AnalysisCaseSourceResponse(
                source.getAnalysisCaseSourceId(),
                extraction.getExtractionId(),
                document.getDocumentId(),
                source.getDocumentType(),
                document.getDisplayName(),
                extraction.getMajorVersion(),
                extraction.getMinorVersion()
        );
    }
}