package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "action_plan", uniqueConstraints = {
        @UniqueConstraint(name = "uk_action_plan_snapshot_task", columnNames = {"snapshot_id", "task_key"})
})
public class ActionPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long actionPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private AnalysisInputSnapshot snapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchAnalysisResult matchAnalysisResult;

    // AI taskId는 DB PK가 아니라 snapshot 안에서만 유일한 taskKey로 저장한다.
    @Column(name = "task_key", nullable = false, length = 100)
    private String taskKey;

    @Column(name = "related_requirement_id", nullable = false, length = 100)
    private String relatedRequirementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_level", nullable = false, length = 20)
    private ActionPlanMatchLevel matchLevel;

    @Lob
    @Column(name = "missing_point", nullable = false, columnDefinition = "TEXT")
    private String missingPoint;

    @Lob
    @Column(name = "suggestion", nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    // AI 결과와 사용자가 변경하는 마감·완료 생애주기를 분리한다.
    @Column(name = "deadline")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActionPlanStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id", nullable = false)
    private AiCallLog aiCallLog;

    public static ActionPlan from(AnalysisInputSnapshot snapshot, MatchAnalysisResult matchAnalysisResult,
                                  AiCallLog aiCallLog, CustomizedAnalysisGenerationResult.Task value) {
        ActionPlan plan = new ActionPlan();
        plan.snapshot = snapshot;
        plan.matchAnalysisResult = matchAnalysisResult;
        plan.aiCallLog = aiCallLog;
        plan.taskKey = value.getTaskId();
        plan.relatedRequirementId = value.getRelatedRequirementId();
        plan.matchLevel = value.getMatchLevel();
        plan.missingPoint = value.getMissingPoint();
        plan.suggestion = value.getSuggestion();
        plan.status = ActionPlanStatus.PENDING;
        return plan;
    }
}
