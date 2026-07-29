package com.example.jobpuzzle.admin.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

// 가이드 원문을 검색용 청크로 나눈다.
// analysis.service.AnalysisMaterialChunkService와 같은 분할 기준(페이지 경계 우선 -> 문단 우선 -> 최대 길이 병합/절단)을
// 독립적으로 다시 구현한 것 - 그 클래스는 private 메서드와 analysis 엔티티에 강하게 묶여 있어 그대로 재사용할 수 없었다.
public final class GuideTextChunker {

    public static final int MAX_CHUNK_LENGTH = 2_000;
    private static final Pattern PAGE_MARKER_PATTERN = Pattern.compile("\\[(\\d+)페이지]\\n");
    private static final Pattern PARAGRAPH_SEPARATOR_PATTERN = Pattern.compile("\\R[\\t ]*\\R+");

    private GuideTextChunker() {
    }

    public static List<String> split(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (TextRange page : splitPages(content)) {
            for (TextRange range : splitPageIntoChunks(content, page)) {
                chunks.add(content.substring(range.start(), range.end()));
            }
        }
        return chunks;
    }

    // [N페이지] 마커가 있으면 그 경계를 페이지 범위로, 없으면 전체를 페이지 1개로 취급한다.
    private static List<TextRange> splitPages(String content) {
        List<MatchResult> markers = PAGE_MARKER_PATTERN.matcher(content).results().toList();
        if (markers.isEmpty()) {
            TextRange range = trimRange(content, 0, content.length());
            return range == null ? List.of() : List.of(range);
        }
        List<TextRange> pages = new ArrayList<>();
        for (int index = 0; index < markers.size(); index++) {
            MatchResult marker = markers.get(index);
            int end = index + 1 < markers.size() ? markers.get(index + 1).start() : content.length();
            TextRange range = trimRange(content, marker.end(), end);
            if (range != null) {
                pages.add(range);
            }
        }
        return pages;
    }

    // 빈 줄을 우선 경계로 쓰고, 작은 문단은 최대 길이까지 이어붙이며, 문단 자체가 길면 고정 길이로 자른다.
    private static List<TextRange> splitPageIntoChunks(String original, TextRange page) {
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
        if (pending != null) {
            chunks.add(pending);
        }
        return chunks;
    }

    private static List<TextRange> paragraphs(String original, int start, int end) {
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

    private static void addTrimmedRange(List<TextRange> target, String original, int start, int end) {
        TextRange range = trimRange(original, start, end);
        if (range != null) {
            target.add(range);
        }
    }

    private static TextRange trimRange(String value, int start, int end) {
        while (start < end && Character.isWhitespace(value.charAt(start))) start++;
        while (end > start && Character.isWhitespace(value.charAt(end - 1))) end--;
        return start == end ? null : new TextRange(start, end);
    }

    private record TextRange(int start, int end) {
        int length() {
            return end - start;
        }
    }
}