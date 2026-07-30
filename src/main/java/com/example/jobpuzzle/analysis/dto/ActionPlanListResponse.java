package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.analysis.entity.ActionPlan;
import com.example.jobpuzzle.analysis.entity.ActionPlanMatchLevel;
import com.example.jobpuzzle.analysis.entity.ActionPlanStatus;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ActionPlanListResponse {

    private final Long actionPlanId;
    private final Long analysisCaseId;
    private final Long snapshotId;
    private final String mainCategory;
    private final String subCategory;
    private final JobCategoryCareerLevel careerLevel;
    private final String relatedRequirementId;
    private final String requirement;
    private final ActionPlanMatchLevel matchLevel;
    private final String missingPoint;
    private final String suggestion;
    private final ActionPlanStatus status;
    private final LocalDate deadline;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;

    private ActionPlanListResponse(ActionPlan actionPlan) {
        var snapshot = actionPlan.getSnapshot();
        var jobCategory = snapshot.getJobCategory();
        this.actionPlanId = actionPlan.getActionPlanId();
        this.analysisCaseId = snapshot.getAnalysisCase().getAnalysisCaseId();
        this.snapshotId = snapshot.getSnapshotId();
        this.mainCategory = jobCategory.getMainCategory();
        this.subCategory = jobCategory.getSubCategory();
        this.careerLevel = jobCategory.getCareerLevel();
        this.relatedRequirementId = actionPlan.getRelatedRequirementId();
        this.requirement = actionPlan.getMatchAnalysisResult().getRequirement();
        this.matchLevel = actionPlan.getMatchLevel();
        this.missingPoint = actionPlan.getMissingPoint();
        this.suggestion = actionPlan.getSuggestion();
        this.status = actionPlan.getStatus();
        this.deadline = actionPlan.getDeadline();
        this.completedAt = actionPlan.getCompletedAt();
        this.createdAt = actionPlan.getCreatedAt();
    }

    // 액션플랜 관리 화면에 필요한 AI 과제 정보와 사용자 관리 상태만 응답으로 변환한다.
    public static ActionPlanListResponse from(ActionPlan actionPlan) {
        return new ActionPlanListResponse(actionPlan);
    }
}
