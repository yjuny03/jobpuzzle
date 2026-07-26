package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshotSource;
import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalChunkRepository;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalResultRepository;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * 확정 snapshot source 하나를 페이지 경계 안에서 결정적으로 나누고 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class AnalysisMaterialChunkService {

    // 현재 분할 규칙의 식별값으로 재실행 시 동일 규칙 청크만 재사용하게 한다.
    public static final String CURRENT_CHUNKING_VERSION = "v1";
    static final int MAX_CHUNK_LENGTH = 2_000;
    private static final Pattern PAGE_MARKER_PATTERN = Pattern.compile("\\[(\\d+)페이지]\\n");
    private static final Pattern PARAGRAPH_SEPARATOR_PATTERN = Pattern.compile("\\R[\\t ]*\\R+");

    private final AnalysisMaterialChunkRepository analysisMaterialChunkRepository;
    private final RequirementRetrievalChunkRepository retrievalChunkRepository;
    private final RequirementRetrievalResultRepository retrievalResultRepository;

    // 확정 source의 정상 청크는 재사용하고, 손상된 집합만 같은 트랜잭션에서 완전히 재생성한다.
    @Transactional
    public List<AnalysisMaterialChunk> getOrCreateChunks(AnalysisInputSnapshotSource snapshotSource, String chunkingVersion) {
        validateInput(snapshotSource, chunkingVersion);
        Long sourceId = snapshotSource.getSnapshotSourceId();
        if (analysisMaterialChunkRepository.existsBySnapshotSource_SnapshotSourceIdAndChunkingVersion(sourceId, chunkingVersion)) {
            List<AnalysisMaterialChunk> existing = analysisMaterialChunkRepository
                    .findBySnapshotSource_SnapshotSourceIdAndChunkingVersionOrderByChunkIndexAsc(sourceId, chunkingVersion);
            if (isCompleteAndValid(existing, snapshotSource, chunkingVersion)) {
                return existing;
            }
            // 원문 청크가 바뀌면 기존 검색 근거도 유효하지 않으므로 함께 제거한다.
            deleteRetrievalsReferencingSourceVersion(sourceId, chunkingVersion);
            // retrieval FK를 정리한 뒤 손상된 source·version 청크만 삭제한다.
            analysisMaterialChunkRepository.deleteBySnapshotSource_SnapshotSourceIdAndChunkingVersion(sourceId, chunkingVersion);
        }

        List<AnalysisMaterialChunk> created = createChunks(snapshotSource, chunkingVersion);
        return created.isEmpty() ? List.of() : analysisMaterialChunkRepository.saveAll(created);
    }

    // 같은 source·version material chunk를 참조한 retrieval 전체를 result 단위로 무효화한다.
    private void deleteRetrievalsReferencingSourceVersion(Long snapshotSourceId, String chunkingVersion) {
        List<Long> retrievalResultIds = retrievalChunkRepository
                .findDistinctRetrievalResultIdsByMaterialChunkSnapshotSourceIdAndChunkingVersion(snapshotSourceId, chunkingVersion);
        if (retrievalResultIds.isEmpty()) return;
        // retrieval FK를 먼저 제거해 material chunk 삭제 시 제약 위반을 막는다.
        retrievalChunkRepository.deleteByRetrievalResult_RetrievalResultIdIn(retrievalResultIds);
        // 부분 retrieval 결과를 남기지 않도록 무효화 대상 result 전체를 제거한다.
        retrievalResultRepository.deleteByRetrievalResultIdIn(retrievalResultIds);
        // 같은 트랜잭션 안에서 FK 삭제를 DB에 반영한 뒤 material chunk를 재생성한다.
        retrievalResultRepository.flush();
    }

    // CONFIRMED 원문과 동일 사용자·문서 유형 관계만 chunk 생성 입력으로 허용한다.
    private void validateInput(AnalysisInputSnapshotSource source, String chunkingVersion) {
        if (source == null || source.getSnapshotSourceId() == null || source.getSnapshot() == null
                || source.getExtraction() == null || blank(chunkingVersion)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "invalid analysis material chunk input");
        }
        AnalysisInputSnapshot snapshot = source.getSnapshot();
        DocumentExtraction extraction = source.getExtraction();
        if (snapshot.getUser() == null || snapshot.getUser().getUserId() == null || extraction.getDocument() == null
                || extraction.getDocument().getUser() == null || extraction.getDocument().getUser().getUserId() == null
                || source.getDocumentType() == null || extraction.getDocument().getDocumentType() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "incomplete snapshot source relation");
        }
        if (extraction.getVersionStatus() != DocumentVersionStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.EXTRACTION_NOT_CONFIRMED);
        }
        if (!snapshot.getUser().getUserId().equals(extraction.getDocument().getUser().getUserId())
                || source.getDocumentType() != extraction.getDocument().getDocumentType()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "snapshot source ownership or type mismatch");
        }
    }

    // 기존 청크가 0부터 연속되고 원문·관계·해시까지 모두 맞는 경우에만 재사용한다.
    private boolean isCompleteAndValid(List<AnalysisMaterialChunk> chunks, AnalysisInputSnapshotSource source, String version) {
        if (chunks.isEmpty()) return false;
        String original = source.getExtraction().getContent();
        if (original == null) return false;
        for (int index = 0; index < chunks.size(); index++) {
            AnalysisMaterialChunk chunk = chunks.get(index);
            if (chunk.getChunkIndex() != index || !sameId(chunk.getSnapshotSource().getSnapshotSourceId(), source.getSnapshotSourceId())
                    || !sameId(chunk.getSnapshot().getSnapshotId(), source.getSnapshot().getSnapshotId())
                    || !sameId(chunk.getExtraction().getExtractionId(), source.getExtraction().getExtractionId())
                    || !version.equals(chunk.getChunkingVersion()) || chunk.getCharStart() < 0
                    || chunk.getCharEnd() <= chunk.getCharStart() || chunk.getCharEnd() > original.length()) {
                return false;
            }
            String expected = original.substring(chunk.getCharStart(), chunk.getCharEnd());
            if (!expected.equals(chunk.getContent()) || !hash(expected, version).equals(chunk.getContentHash())) {
                return false;
            }
        }
        return true;
    }

    // 페이지별 문단 우선 분할을 수행해 page를 넘지 않는 순서 고정 청크를 만든다.
    private List<AnalysisMaterialChunk> createChunks(AnalysisInputSnapshotSource source, String version) {
        String original = source.getExtraction().getContent();
        if (original == null || original.isBlank()) return List.of();
        List<AnalysisMaterialChunk> chunks = new ArrayList<>();
        for (PageRange page : splitPages(original)) {
            for (TextRange range : splitPageIntoChunks(original, page)) {
                String content = original.substring(range.start(), range.end());
                chunks.add(AnalysisMaterialChunk.create(source.getSnapshot(), source, source.getExtraction(),
                        source.getSnapshot().getUser().getUserId(), source.getExtraction().getDocument().getDocumentId(),
                        source.getDocumentType(), page.pageNumber(), page.pageNumber(), range.start(), range.end(), chunks.size(),
                        content, hash(content, version), version));
            }
        }
        return chunks;
    }

    // AnalysisCaseService.splitPages와 같은 marker 해석을 사용하되 원문 offset을 보존한다.
    private List<PageRange> splitPages(String content) {
        List<MatchResult> markers = PAGE_MARKER_PATTERN.matcher(content).results().toList();
        if (markers.isEmpty()) {
            TextRange range = trimRange(content, 0, content.length());
            return range == null ? List.of() : List.of(new PageRange(1, range.start(), range.end()));
        }
        List<PageRange> pages = new ArrayList<>();
        for (int index = 0; index < markers.size(); index++) {
            MatchResult marker = markers.get(index);
            int end = index + 1 < markers.size() ? markers.get(index + 1).start() : content.length();
            TextRange range = trimRange(content, marker.end(), end);
            if (range != null) pages.add(new PageRange(Integer.parseInt(marker.group(1)), range.start(), range.end()));
        }
        return pages;
    }

    // 빈 줄을 우선 경계로 사용하고, 한 문단이 길면 고정 문자 길이로만 잘라 offset을 보존한다.
    private List<TextRange> splitPageIntoChunks(String original, PageRange page) {
        List<TextRange> paragraphs = paragraphs(original, page.start(), page.end());
        List<TextRange> chunks = new ArrayList<>();
        TextRange pending = null;
        for (TextRange paragraph : paragraphs) {
            if (paragraph.length() > MAX_CHUNK_LENGTH) {
                if (pending != null) {
                    chunks.add(pending);
                    pending = null;
                }
                for (int start = paragraph.start(); start < paragraph.end(); start += MAX_CHUNK_LENGTH) {
                    chunks.add(new TextRange(start, Math.min(start + MAX_CHUNK_LENGTH, paragraph.end())));
                }
            } else if (pending == null) {
                pending = paragraph;
            } else if (paragraph.end() - pending.start() <= MAX_CHUNK_LENGTH) {
                pending = new TextRange(pending.start(), paragraph.end());
            } else {
                chunks.add(pending);
                pending = paragraph;
            }
        }
        if (pending != null) chunks.add(pending);
        return chunks;
    }

    // 페이지 내 빈 줄 delimiter로 문단 범위를 나누되 저장용 원문 문자열은 수정하지 않는다.
    private List<TextRange> paragraphs(String original, int start, int end) {
        List<TextRange> values = new ArrayList<>();
        String pageText = original.substring(start, end);
        List<MatchResult> separators = PARAGRAPH_SEPARATOR_PATTERN.matcher(pageText).results().toList();
        int paragraphStart = start;
        for (MatchResult separator : separators) {
            addTrimmedRange(values, original, paragraphStart, start + separator.start());
            paragraphStart = start + separator.end();
        }
        addTrimmedRange(values, original, paragraphStart, end);
        return values;
    }

    // 원문 인덱스 범위만 trim해 AnalysisCaseService의 페이지 text.trim() 의미를 맞춘다.
    private void addTrimmedRange(List<TextRange> target, String original, int start, int end) {
        TextRange range = trimRange(original, start, end);
        if (range != null) target.add(range);
    }

    private TextRange trimRange(String value, int start, int end) {
        while (start < end && Character.isWhitespace(value.charAt(start))) start++;
        while (end > start && Character.isWhitespace(value.charAt(end - 1))) end--;
        return start == end ? null : new TextRange(start, end);
    }

    // 원문 내용과 분할 규칙 버전으로 SHA-256 해시를 만들어 같은 입력의 결과를 고정한다.
    private String hash(String content, String version) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((version + "\\n" + content).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean sameId(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private record PageRange(int pageNumber, int start, int end) {
    }

    private record TextRange(int start, int end) {
        int length() {
            return end - start;
        }
    }
}
