package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.Getter;

import java.util.List;

// JSON-03: AI 실행 직전에만 조립하는 서버 내부 컨텍스트. 공개 API 응답(AnalysisInputSnapshotResponse)과
// 완전히 분리해서 유지하며, userId는 포함하지 않는다
@Getter
public class AnalysisInputSnapshotContext {

    private final Long snapshotId;
    private final Long analysisCaseId;
    private final String mainCategory;
    private final String subCategory;
    private final JobCategoryCareerLevel careerLevel;
    private final List<AnalysisInputSnapshotContextSource> sources;

    private AnalysisInputSnapshotContext(
            Long snapshotId, Long analysisCaseId, String mainCategory, String subCategory,
            JobCategoryCareerLevel careerLevel, List<AnalysisInputSnapshotContextSource> sources
    ) {
        this.snapshotId = snapshotId;
        this.analysisCaseId = analysisCaseId;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.careerLevel = careerLevel;
        this.sources = sources;
    }

    public static AnalysisInputSnapshotContext of(AnalysisInputSnapshot snapshot, List<AnalysisInputSnapshotContextSource> sources) {
        var jobCategory = snapshot.getJobCategory();
        return new AnalysisInputSnapshotContext(
                snapshot.getSnapshotId(),
                snapshot.getAnalysisCase().getAnalysisCaseId(),
                jobCategory.getMainCategory(),
                jobCategory.getSubCategory(),
                jobCategory.getCareerLevel(),
                sources
        );
    }
}
