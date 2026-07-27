package com.example.jobpuzzle.admin.dto;

import com.example.jobpuzzle.guide.entity.GuideScopeType;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentStatus;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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

    private AdminGuideResponse(
            Long guideId, Long previousGuideId, String guideCode, String title, GuideScopeType scopeType,
            String mainCategory, String subCategory, JobCategoryCareerLevel careerLevel, String scopeMainCategory,
            JobGuideDocumentStatus status, String version, JobGuideDocumentSourceType sourceType, boolean hasFile,
            String applicableScope, List<String> evaluationFocus, List<String> evidenceRules,
            List<String> questionDirection, List<String> avoidQuestions,
            String createdByLoginId, LocalDateTime createdAt
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
    }

    public static AdminGuideResponse from(JobGuideDocument guide) {
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
                guide.getCreatedAt()
        );
    }
}