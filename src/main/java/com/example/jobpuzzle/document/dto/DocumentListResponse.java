package com.example.jobpuzzle.document.dto;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Getter
public class DocumentListResponse {

    // DocumentExtractionAsyncRunner가 content에 심어두는 "[N페이지]\n" 마커. AnalysisCaseService.splitPages, document-flow.js의 splitPages와 동일 규칙
    private static final Pattern PAGE_MARKER_PATTERN = Pattern.compile("\\[(\\d+)페이지]\\n");

    private final Long documentId;
    private final UserDocumentType documentType;
    private final String displayName;
    private final UserDocumentSourceType sourceType;
    private final Integer latestMajorVersion;
    private final Integer latestMinorVersion;
    private final DocumentVersionStatus latestVersionStatus;
    private final Integer contentLength;
    private final LocalDateTime deletedAt;
    private final LocalDateTime updatedAt;

    private DocumentListResponse(
            Long documentId, UserDocumentType documentType, String displayName, UserDocumentSourceType sourceType,
            Integer latestMajorVersion, Integer latestMinorVersion, DocumentVersionStatus latestVersionStatus,
            Integer contentLength, LocalDateTime deletedAt, LocalDateTime updatedAt
    ) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.displayName = displayName;
        this.sourceType = sourceType;
        this.latestMajorVersion = latestMajorVersion;
        this.latestMinorVersion = latestMinorVersion;
        this.latestVersionStatus = latestVersionStatus;
        this.contentLength = contentLength;
        this.deletedAt = deletedAt;
        this.updatedAt = updatedAt;
    }

    // latestExtraction은 해당 자료의 최신 저장 버전(있다면 그 버전, 없으면 null)
    public static DocumentListResponse of(UserDocument document, DocumentExtraction latestExtraction) {
        return new DocumentListResponse(
                document.getDocumentId(),
                document.getDocumentType(),
                document.getDisplayName(),
                document.getSourceType(),
                latestExtraction != null ? latestExtraction.getMajorVersion() : null,
                latestExtraction != null ? latestExtraction.getMinorVersion() : null,
                latestExtraction != null ? latestExtraction.getVersionStatus() : null,
                latestExtraction != null ? contentLength(latestExtraction.getContent()) : null,
                document.getDeletedAt(),
                document.getUpdatedAt()
        );
    }

    // 페이지 마커를 뺀 실제 내용 기준 총 글자 수 (document-flow.js totalContentLength와 동일 규칙)
    private static int contentLength(String content) {
        if (content == null || content.isBlank()) return 0;
        var matches = PAGE_MARKER_PATTERN.matcher(content).results().toList();
        if (matches.isEmpty()) return content.trim().length();
        int total = 0;
        for (int i = 0; i < matches.size(); i++) {
            MatchResult match = matches.get(i);
            int start = match.end();
            int end = (i + 1 < matches.size()) ? matches.get(i + 1).start() : content.length();
            total += content.substring(start, end).trim().length();
        }
        return total;
    }
}