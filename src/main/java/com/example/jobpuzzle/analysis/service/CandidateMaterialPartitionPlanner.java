package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.validation.AnalysisSourceMarkerParser;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

// JSON-02 입력을 extraction 우선·marker 순서 기준으로 나눈다. 사실을 버리거나 marker를 자르지 않는다.
@Component
public class CandidateMaterialPartitionPlanner {

    private final AnalysisSourceMarkerParser markerParser;

    public CandidateMaterialPartitionPlanner(AnalysisSourceMarkerParser markerParser) {
        this.markerParser = markerParser;
    }

    public List<CandidateMaterialMarkerPartition> plan(List<AnalysisInputSnapshotContextSource> sources, Limits limits) {
        if (limits.maxMarkerContentChars() <= 0 || limits.maxMarkers() <= 0) {
            throw new IllegalArgumentException("partition limits must be positive");
        }
        List<CandidateMaterialMarkerPartition> partitions = new ArrayList<>();
        int ordinal = 0;
        for (AnalysisInputSnapshotContextSource source : sources) {
            List<AnalysisSourceMarkerParser.SourceMarker> markers = markerParser.parseSources(List.of(source));
            int partitionIndex = 0;
            List<AnalysisSourceMarkerParser.SourceMarker> current = new ArrayList<>();
            int currentChars = 0;
            for (AnalysisSourceMarkerParser.SourceMarker marker : markers) {
                int markerChars = marker.segmentText() == null ? 0 : marker.segmentText().length();
                boolean reachesLimit = !current.isEmpty() && (current.size() >= limits.maxMarkers()
                        || currentChars + markerChars > limits.maxMarkerContentChars());
                if (reachesLimit) {
                    partitions.add(partition(source, current, currentChars, partitionIndex++, ordinal++, limits));
                    current = new ArrayList<>();
                    currentChars = 0;
                }
                // 단일 marker가 한도를 초과해도 분할하지 않는다. executor가 명시적으로 실패 처리할 수 있게 표시만 한다.
                current.add(marker);
                currentChars += markerChars;
            }
            if (!current.isEmpty()) {
                partitions.add(partition(source, current, currentChars, partitionIndex, ordinal++, limits));
            }
        }
        return List.copyOf(partitions);
    }

    private CandidateMaterialMarkerPartition partition(AnalysisInputSnapshotContextSource source,
                                                       List<AnalysisSourceMarkerParser.SourceMarker> markers,
                                                       int markerChars, int partitionIndex, int ordinal, Limits limits) {
        return new CandidateMaterialMarkerPartition(source.getDocumentType(), source.getExtractionId(), source.getDocumentId(),
                partitionIndex, ordinal, markers, markerChars,
                markers.size() == 1 && markerChars > limits.maxMarkerContentChars(), hash(markers));
    }

    // ID·페이지·segment·원문을 모두 hash에 넣어, 원문 변경과 marker 재배치를 재사용 대상에서 제외한다.
    private String hash(List<AnalysisSourceMarkerParser.SourceMarker> markers) {
        String material = markers.stream().map(marker -> marker.extractionId() + "|" + marker.documentId() + "|"
                        + marker.documentType() + "|" + marker.pageNumber() + "|" + marker.segmentId() + "|" + marker.segmentText())
                .reduce("", (left, right) -> left + "\n" + right);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    public record Limits(int maxMarkerContentChars, int maxMarkers) { }
}
