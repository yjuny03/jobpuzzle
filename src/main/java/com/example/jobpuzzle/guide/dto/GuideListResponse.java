package com.example.jobpuzzle.guide.dto;

import com.example.jobpuzzle.guide.entity.GuideScopeType;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentStatus;
import com.example.jobpuzzle.guide.entity.GuidePreprocessingStatus;
import com.example.jobpuzzle.guide.entity.GuideIndexingStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Builder;

@Getter
@Builder
public class GuideListResponse {
    private final Long guideId;
    private final Long previousGuideId;
    private final String guideCode;
    private final String version;
    private final String title;
    private final GuideScopeType scopeType;
    private final Long jobCategoryId;
    private final String scopeMainCategory;
    private final JobGuideDocumentStatus status;
    private final int chunkCount;
    private final GuidePreprocessingStatus preprocessingStatus;
    private final String preprocessingModel;
    private final LocalDateTime preprocessedAt;
    private final String preprocessingError;
    private final GuideIndexingStatus indexingStatus;
    private final String embeddingProvider;
    private final String embeddingModel;
    private final Integer embeddingDimension;
    private final LocalDateTime indexedAt;
    private final String indexingError;

    public static GuideListResponse from(JobGuideDocument guide, int chunkCount) {
        return GuideListResponse.builder()
                .guideId(guide.getGuideId())
                .previousGuideId(guide.getPreviousGuide() == null ? null : guide.getPreviousGuide().getGuideId())
                .guideCode(guide.getGuideCode())
                .version(guide.getVersion())
                .title(guide.getTitle())
                .scopeType(guide.getScopeType())
                .jobCategoryId(guide.getJobCategory() == null ? null : guide.getJobCategory().getJobCategoryId())
                .scopeMainCategory(guide.getScopeMainCategory())
                .status(guide.getStatus())
                .chunkCount(chunkCount)
                .preprocessingStatus(guide.getPreprocessingStatus())
                .preprocessingModel(guide.getPreprocessingModel())
                .preprocessedAt(guide.getPreprocessedAt())
                .preprocessingError(guide.getPreprocessingError())
                .indexingStatus(guide.getIndexingStatus())
                .embeddingProvider(guide.getEmbeddingProvider())
                .embeddingModel(guide.getEmbeddingModel())
                .embeddingDimension(guide.getEmbeddingDimension())
                .indexedAt(guide.getIndexedAt())
                .indexingError(guide.getIndexingError())
                .build();
    }
}
