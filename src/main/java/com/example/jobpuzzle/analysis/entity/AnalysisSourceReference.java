package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI 응답 DTO와 DB JSON 저장 구조의 결합을 끊는 근거 참조 저장 값이다.
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisSourceReference {

    private Long extractionId;
    private Long documentId;
    private UserDocumentType documentType;
    private Integer pageNumber;
    private String segmentId;
    private String evidenceText;

    public static AnalysisSourceReference from(SourceReference source) {
        if (source == null) {
            return null;
        }
        return AnalysisSourceReference.builder()
                .extractionId(source.getExtractionId())
                .documentId(source.getDocumentId())
                .documentType(source.getDocumentType())
                .pageNumber(source.getPageNumber())
                .segmentId(source.getSegmentId())
                .evidenceText(source.getEvidenceText())
                .build();
    }
}
