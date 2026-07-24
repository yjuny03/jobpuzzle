package com.example.jobpuzzle.ai.validation;

import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// JSON-03 analysisText의 opaque marker를 공통 근거 위치 모델로 해석한다.
@Component
public class AnalysisSourceMarkerParser {

    private static final Pattern MARKER = Pattern.compile(
            "\\[SOURCE extractionId=(\\d+) documentId=(\\d+)]\\[PAGE=(\\d+|null)]\\[SEGMENT=([^]]+)]"
    );
    private static final Pattern DOCUMENT_SECTION = Pattern.compile(
            "^\\[(JOB_POSTING|COMPANY_INFO|RESUME|COVER_LETTER|PORTFOLIO|EXPERIENCE_NOTE)]$"
    );

    // 컨텍스트별 analysisText를 읽어 문서 유형이 확정된 marker 목록을 만든다.
    public List<SourceMarker> parseSources(List<AnalysisInputSnapshotContextSource> sources) {
        List<SourceMarker> markers = new ArrayList<>();
        for (AnalysisInputSnapshotContextSource source : sources) {
            markers.addAll(parse(source.getDocumentType(), source.getAnalysisText()));
        }
        return markers;
    }

    // Mock Provider도 실제 렌더링 프롬프트의 문서 섹션과 marker만 재사용한다.
    public List<SourceMarker> parsePrompt(String prompt) {
        List<SourceMarker> markers = new ArrayList<>();
        UserDocumentType currentType = null;
        String[] lines = prompt.replace("\r\n", "\n").split("\n");
        StringBuilder currentBlock = new StringBuilder();
        for (String line : lines) {
            Matcher section = DOCUMENT_SECTION.matcher(line.trim());
            if (section.matches()) {
                markers.addAll(parse(currentType, currentBlock.toString()));
                currentBlock.setLength(0);
                currentType = UserDocumentType.valueOf(section.group(1));
            } else if (currentType != null) {
                currentBlock.append(line).append("\n");
            }
        }
        markers.addAll(parse(currentType, currentBlock.toString()));
        return markers;
    }

    public List<UserDocumentType> documentTypesInPrompt(String prompt) {
        List<UserDocumentType> types = new ArrayList<>();
        for (String line : prompt.replace("\r\n", "\n").split("\n")) {
            Matcher section = DOCUMENT_SECTION.matcher(line.trim());
            if (section.matches()) {
                UserDocumentType type = UserDocumentType.valueOf(section.group(1));
                if (!types.contains(type)) {
                    types.add(type);
                }
            }
        }
        return types;
    }

    private List<SourceMarker> parse(UserDocumentType documentType, String text) {
        if (documentType == null || text == null) {
            return List.of();
        }
        List<SourceMarker> markers = new ArrayList<>();
        List<MarkerMatch> matches = new ArrayList<>();
        Matcher matcher = MARKER.matcher(text);
        while (matcher.find()) {
            matches.add(new MarkerMatch(
                    matcher.start(), matcher.end(), matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)
            ));
        }
        for (int index = 0; index < matches.size(); index++) {
            MarkerMatch match = matches.get(index);
            int textEnd = index + 1 < matches.size() ? matches.get(index + 1).start() : text.length();
            markers.add(new SourceMarker(
                    Long.valueOf(match.extractionId()),
                    Long.valueOf(match.documentId()),
                    documentType,
                    "null".equals(match.pageNumber()) ? null : Integer.valueOf(match.pageNumber()),
                    "null".equals(match.segmentId()) ? null : match.segmentId(),
                    text.substring(match.end(), textEnd).trim()
            ));
        }
        return markers;
    }

    public record SourceMarker(
            Long extractionId,
            Long documentId,
            UserDocumentType documentType,
            Integer pageNumber,
            String segmentId,
            String segmentText
    ) {
    }

    private record MarkerMatch(
            int start, int end, String extractionId, String documentId, String pageNumber, String segmentId
    ) {
    }
}
