package com.example.jobpuzzle.admin.dto;

import com.example.jobpuzzle.guide.entity.GuideScopeType;
import com.example.jobpuzzle.guide.entity.GuideIndexingStatus;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentStatus;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 화면이 사용하는 가이드 통합 응답.
 *
 * <p>기존 등록·버전 정보와 신규 AI 전처리·벡터 색인 상태를 한 응답에 담아,
 * 화면이 서로 다른 두 API 결과를 임의로 조합하지 않도록 한다.</p>
 */
@Getter
public class AdminGuideResponse {

    private final Long guideId;
    private final Long previousGuideId;
    private final String guideCode;
    private final String title;
    private final GuideScopeType scopeType;
    private final String mainCategory;
    private final String subCategory;
    private final JobCategoryCareerLevel careerLevel;
    private final String scopeMainCategory;
    private final JobGuideDocumentStatus status;
    private final String version;
    private final JobGuideDocumentSourceType sourceType;
    private final boolean hasFile;
    private final String applicableScope;
    private final List<String> evaluationFocus;
    private final List<String> evidenceRules;
    private final List<String> questionDirection;
    private final List<String> avoidQuestions;
    private final String createdByLoginId;
    private final LocalDateTime createdAt;
    private final long chunkCount;

    // 활성화 가능 여부와 재시도 UI를 서버 상태만으로 판단하기 위한 파이프라인 메타데이터다.
    private final GuideIndexingStatus indexingStatus;
    private final String embeddingProvider;
    private final String embeddingModel;
    private final Integer embeddingDimension;
    private final LocalDateTime indexedAt;
    private final String indexingError;

    private AdminGuideResponse(
            Long guideId, Long previousGuideId, String guideCode, String title, GuideScopeType scopeType,
            String mainCategory, String subCategory, JobCategoryCareerLevel careerLevel, String scopeMainCategory,
            JobGuideDocumentStatus status, String version, JobGuideDocumentSourceType sourceType, boolean hasFile,
            String applicableScope, List<String> evaluationFocus, List<String> evidenceRules,
            List<String> questionDirection, List<String> avoidQuestions,
            String createdByLoginId, LocalDateTime createdAt, long chunkCount,
            GuideIndexingStatus indexingStatus, String embeddingProvider,
            String embeddingModel, Integer embeddingDimension,
            LocalDateTime indexedAt, String indexingError
    ) {
        this.guideId = guideId;
        this.previousGuideId = previousGuideId;
        this.guideCode = guideCode;
        this.title = title;
        this.scopeType = scopeType;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.careerLevel = careerLevel;
        this.scopeMainCategory = scopeMainCategory;
        this.status = status;
        this.version = version;
        this.sourceType = sourceType;
        this.hasFile = hasFile;
        this.applicableScope = applicableScope;
        this.evaluationFocus = evaluationFocus;
        this.evidenceRules = evidenceRules;
        this.questionDirection = questionDirection;
        this.avoidQuestions = avoidQuestions;
        this.createdByLoginId = createdByLoginId;
        this.createdAt = createdAt;
        this.chunkCount = chunkCount;
        this.indexingStatus = indexingStatus;
        this.embeddingProvider = embeddingProvider;
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        this.indexedAt = indexedAt;
        this.indexingError = indexingError;
    }

    public static AdminGuideResponse from(JobGuideDocument guide, long chunkCount) {
        return new AdminGuideResponse(
                guide.getGuideId(),
                guide.getPreviousGuide() != null ? guide.getPreviousGuide().getGuideId() : null,
                guide.getGuideCode(),
                guide.getTitle(),
                guide.getScopeType(),
                guide.getJobCategory() != null ? guide.getJobCategory().getMainCategory() : null,
                guide.getJobCategory() != null ? guide.getJobCategory().getSubCategory() : null,
                guide.getJobCategory() != null ? guide.getJobCategory().getCareerLevel() : null,
                guide.getScopeMainCategory(),
                guide.getStatus(),
                guide.getVersion(),
                guide.getSourceType(),
                guide.getFilePath() != null,
                guide.getApplicableScope(),
                guide.getEvaluationFocus(),
                guide.getEvidenceRules(),
                guide.getQuestionDirection(),
                guide.getAvoidQuestions(),
                guide.getCreatedBy().getLoginId(),
                guide.getCreatedAt(),
                chunkCount,
                guide.getIndexingStatus(),
                guide.getEmbeddingProvider(),
                guide.getEmbeddingModel(),
                guide.getEmbeddingDimension(),
                guide.getIndexedAt(),
                guide.getIndexingError()
        );
    }
}
