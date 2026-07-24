package com.example.jobpuzzle.guide.dto;

import com.example.jobpuzzle.guide.entity.GuideContextChunk;
import com.example.jobpuzzle.guide.entity.GuideContextResult;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GuideContextResultDto {
    private final Long guideId;
    private final String title;
    private final String version;
    private final GuideMatchType matchType;
    private final String mainCategory;
    private final String subCategory;
    private final String careerLevel;
    private final String applicableScope;
    private final List<String> evaluationFocus;
    private final List<String> evidenceRules;
    private final List<String> questionDirection;
    private final List<String> avoidQuestions;
    private final List<Chunk> chunks;
    private final boolean fallbackApplied;
    private final boolean insufficient;
    private final String insufficientReason;

    public static GuideContextResultDto from(GuideContextResult result, List<GuideContextChunk> chunks) {
        return GuideContextResultDto.builder()
                .guideId(result.getGuide() == null ? null : result.getGuide().getGuideId())
                .title(result.getGuide() == null ? null : result.getGuide().getTitle())
                .version(result.getGuideVersion())
                .matchType(result.getMatchType())
                .mainCategory(result.getJobCategory().getMainCategory())
                .subCategory(result.getJobCategory().getSubCategory())
                .careerLevel(result.getCareerLevel().name())
                .applicableScope(result.getApplicableScope())
                .evaluationFocus(orEmpty(result.getEvaluationFocus()))
                .evidenceRules(orEmpty(result.getEvidenceRules()))
                .questionDirection(orEmpty(result.getQuestionDirection()))
                .avoidQuestions(orEmpty(result.getAvoidQuestions()))
                .chunks(chunks.stream().map(Chunk::from).toList())
                .fallbackApplied(result.isFallbackApplied())
                .insufficient(result.isInsufficient())
                .insufficientReason(result.getInsufficientReason())
                .build();
    }

    private static List<String> orEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    @Getter
    @Builder
    public static class Chunk {
        private final Long chunkId;
        private final String title;
        private final String contentSummary;
        private final String content;
        private final Double score;

        private static Chunk from(GuideContextChunk contextChunk) {
            return Chunk.builder()
                    .chunkId(contextChunk.getJobGuideChunk().getChunkId())
                    .title(contextChunk.getJobGuideChunk().getTitle())
                    .contentSummary(contextChunk.getJobGuideChunk().getContentSummary())
                    .content(contextChunk.getJobGuideChunk().getContent())
                    .score(contextChunk.getSimilarityScore())
                    .build();
        }
    }
}
