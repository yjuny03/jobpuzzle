package com.example.jobpuzzle.admin.dto;

import com.example.jobpuzzle.guide.entity.GuideContextPurpose;
import com.example.jobpuzzle.guide.entity.GuideContextResult;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.Getter;

import java.time.LocalDateTime;

// 가이드 사용 이력 조회 - 개인정보는 최소한(아이디)만 노출
@Getter
public class AdminGuideUsageResponse {

    private final Long guideContextResultId;
    private final String loginId;
    private final GuideContextPurpose purpose;
    private final String mainCategory;
    private final String subCategory;
    private final JobCategoryCareerLevel careerLevel;
    private final Long guideId;
    private final String guideVersion;
    private final GuideMatchType matchType;
    private final boolean fallbackApplied;
    private final boolean insufficient;
    private final String insufficientReason;
    private final LocalDateTime createdAt;

    private AdminGuideUsageResponse(
            Long guideContextResultId, String loginId, GuideContextPurpose purpose,
            String mainCategory, String subCategory, JobCategoryCareerLevel careerLevel,
            Long guideId, String guideVersion, GuideMatchType matchType,
            boolean fallbackApplied, boolean insufficient, String insufficientReason, LocalDateTime createdAt
    ) {
        this.guideContextResultId = guideContextResultId;
        this.loginId = loginId;
        this.purpose = purpose;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.careerLevel = careerLevel;
        this.guideId = guideId;
        this.guideVersion = guideVersion;
        this.matchType = matchType;
        this.fallbackApplied = fallbackApplied;
        this.insufficient = insufficient;
        this.insufficientReason = insufficientReason;
        this.createdAt = createdAt;
    }

    public static AdminGuideUsageResponse from(GuideContextResult result) {
        return new AdminGuideUsageResponse(
                result.getGuideContextResultId(),
                result.getUser().getLoginId(),
                result.getPurpose(),
                result.getJobCategory().getMainCategory(),
                result.getJobCategory().getSubCategory(),
                result.getCareerLevel(),
                result.getGuide() != null ? result.getGuide().getGuideId() : null,
                result.getGuideVersion(),
                result.getMatchType(),
                result.isFallbackApplied(),
                result.isInsufficient(),
                result.getInsufficientReason(),
                result.getCreatedAt()
        );
    }
}