package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseSource;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseStatus;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AnalysisCaseResponse {

    private final Long analysisCaseId;
    private final Long jobCategoryId;
    private final String mainCategory;
    private final String subCategory;
    private final JobCategoryCareerLevel careerLevel;
    private final AnalysisCaseStatus status;
    private final List<AnalysisCaseSourceResponse> sources;
    private final LocalDateTime createdAt;

    private AnalysisCaseResponse(
            Long analysisCaseId, Long jobCategoryId, String mainCategory, String subCategory,
            JobCategoryCareerLevel careerLevel, AnalysisCaseStatus status,
            List<AnalysisCaseSourceResponse> sources, LocalDateTime createdAt
    ) {
        this.analysisCaseId = analysisCaseId;
        this.jobCategoryId = jobCategoryId;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.careerLevel = careerLevel;
        this.status = status;
        this.sources = sources;
        this.createdAt = createdAt;
    }

    public static AnalysisCaseResponse of(AnalysisCase analysisCase, List<AnalysisCaseSource> sources) {
        var jobCategory = analysisCase.getJobCategory();
        return new AnalysisCaseResponse(
                analysisCase.getAnalysisCaseId(),
                jobCategory.getJobCategoryId(),
                jobCategory.getMainCategory(),
                jobCategory.getSubCategory(),
                jobCategory.getCareerLevel(),
                analysisCase.getStatus(),
                sources.stream().map(AnalysisCaseSourceResponse::from).toList(),
                analysisCase.getCreatedAt()
        );
    }
}