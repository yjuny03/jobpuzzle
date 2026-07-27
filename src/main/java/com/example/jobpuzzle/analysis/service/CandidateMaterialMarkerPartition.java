package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.validation.AnalysisSourceMarkerParser;
import com.example.jobpuzzle.document.entity.UserDocumentType;

import java.util.List;

// JSON-02 한 번에 전달할 marker 묶음이다. marker 원문을 자르지 않고 원래 순서를 그대로 보존한다.
public record CandidateMaterialMarkerPartition(
        UserDocumentType documentType,
        Long extractionId,
        Long documentId,
        int partitionIndex,
        int partitionOrdinal,
        List<AnalysisSourceMarkerParser.SourceMarker> markers,
        int markerContentChars,
        boolean oversizedSingleMarker,
        String markerContentHash
) {
    public CandidateMaterialMarkerPartition {
        markers = List.copyOf(markers);
        if (markers.isEmpty()) throw new IllegalArgumentException("partition markers must not be empty");
    }

    public String firstSegmentId() { return markers.getFirst().segmentId(); }
    public String lastSegmentId() { return markers.getLast().segmentId(); }
}
