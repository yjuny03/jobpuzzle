package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshotSource;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 스냅샷 구조 조회용 응답
// analysisText는 포함하지 않음 - 마스킹된 실행용 텍스트는 AI 실행 시점에 별도로 구성
@Getter
public class AnalysisInputSnapshotResponse {

    private final Long snapshotId;
    private final Long analysisCaseId;
    private final Long userId;
    private final String mainCategory;
    private final String subCategory;
    private final JobCategoryCareerLevel careerLevel;
    private final List<AnalysisInputSnapshotSourceResponse> sources;
    private final LocalDateTime createdAt;

    private AnalysisInputSnapshotResponse(
            Long snapshotId, Long analysisCaseId, Long userId, String mainCategory, String subCategory,
            JobCategoryCareerLevel careerLevel, List<AnalysisInputSnapshotSourceResponse> sources, LocalDateTime createdAt
    ) {
        this.snapshotId = snapshotId;
        this.analysisCaseId = analysisCaseId;
        this.userId = userId;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.careerLevel = careerLevel;
        this.sources = sources;
        this.createdAt = createdAt;
    }

    public static AnalysisInputSnapshotResponse of(AnalysisInputSnapshot snapshot, List<AnalysisInputSnapshotSource> sources) {
        var jobCategory = snapshot.getJobCategory();
        return new AnalysisInputSnapshotResponse(
                snapshot.getSnapshotId(),
                snapshot.getAnalysisCase().getAnalysisCaseId(),
                snapshot.getUser().getUserId(),
                jobCategory.getMainCategory(),
                jobCategory.getSubCategory(),
                jobCategory.getCareerLevel(),
                sources.stream().map(AnalysisInputSnapshotSourceResponse::from).toList(),
                snapshot.getCreatedAt()
        );
    }
}