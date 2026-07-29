package com.example.jobpuzzle.guide.dto;

import java.util.List;

/** OpenAI Structured Outputs가 반환해야 하는 내부 가이드 구조다. */
public record GuidePreprocessingResult(
        String applicableScope,
        List<String> evaluationFocus,
        List<String> evidenceRules,
        List<String> questionDirection,
        List<String> avoidQuestions,
        List<Chunk> chunks
) {
    public record Chunk(String title, String content, String contentSummary) {
    }
}
