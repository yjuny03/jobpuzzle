package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.analysis.entity.ActionPlan;
import com.example.jobpuzzle.analysis.entity.ActionPlanStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ActionPlanCompleteResponse {

    private final Long actionPlanId;
    private final ActionPlanStatus status;
    private final LocalDateTime completedAt;

    private ActionPlanCompleteResponse(ActionPlan actionPlan) {
        this.actionPlanId = actionPlan.getActionPlanId();
        this.status = actionPlan.getStatus();
        this.completedAt = actionPlan.getCompletedAt();
    }

    // 완료 변경 후 클라이언트가 즉시 갱신할 최소 상태만 반환한다.
    public static ActionPlanCompleteResponse from(ActionPlan actionPlan) {
        return new ActionPlanCompleteResponse(actionPlan);
    }
}
