package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshotSource;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.Getter;

// AI 실행 직전 조립되는 내부 컨텍스트용 소스. 공개 응답(AnalysisInputSnapshotSourceResponse)과 달리
// analysisText(마커 포함 원문)를 포함한다. 1단계라 마스킹은 아직 적용하지 않음
@Getter
public class AnalysisInputSnapshotContextSource {

    private final Long extractionId;
    private final Long documentId;
    private final UserDocumentType documentType;
    private final String displayName;
    private final Integer majorVersion;
    private final Integer minorVersion;
    private final String analysisText;

    private AnalysisInputSnapshotContextSource(
            Long extractionId, Long documentId, UserDocumentType documentType,
            String displayName, Integer majorVersion, Integer minorVersion, String analysisText
    ) {
        this.extractionId = extractionId;
        this.documentId = documentId;
        this.documentType = documentType;
        this.displayName = displayName;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.analysisText = analysisText;
    }

    public static AnalysisInputSnapshotContextSource from(AnalysisInputSnapshotSource source, String analysisText) {
        var extraction = source.getExtraction();
        var document = extraction.getDocument();
        return new AnalysisInputSnapshotContextSource(
                extraction.getExtractionId(),
                document.getDocumentId(),
                source.getDocumentType(),
                document.getDisplayName(),
                extraction.getMajorVersion(),
                extraction.getMinorVersion(),
                analysisText
        );
    }
}
