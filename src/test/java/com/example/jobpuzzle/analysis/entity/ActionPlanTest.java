package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.log.AiCallLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ActionPlanTest {

    // 마감일 변경이 완료 상태를 암묵적으로 바꾸지 않는지 검증한다.
    @Test
    void updatesAndRemovesDeadlineWithoutChangingCompletionStatus() {
        ActionPlan actionPlan = actionPlan();
        LocalDate deadline = LocalDate.of(2026, 8, 10);

        actionPlan.updateDeadline(deadline);

        assertThat(actionPlan.getDeadline()).isEqualTo(deadline);
        assertThat(actionPlan.getStatus()).isEqualTo(ActionPlanStatus.PENDING);

        actionPlan.updateDeadline(null);

        assertThat(actionPlan.getDeadline()).isNull();
        assertThat(actionPlan.getStatus()).isEqualTo(ActionPlanStatus.PENDING);
    }

    // 완료와 완료 취소가 status와 completedAt을 한 쌍으로 변경하는지 검증한다.
    @Test
    void completesAndReopensAsOneConsistentLifecycle() {
        ActionPlan actionPlan = actionPlan();
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 30, 12, 0);

        actionPlan.complete(completedAt);

        assertThat(actionPlan.getStatus()).isEqualTo(ActionPlanStatus.DONE);
        assertThat(actionPlan.getCompletedAt()).isEqualTo(completedAt);

        actionPlan.reopen();

        assertThat(actionPlan.getStatus()).isEqualTo(ActionPlanStatus.PENDING);
        assertThat(actionPlan.getCompletedAt()).isNull();
    }

    // 중복 완료 요청이 기존 완료 시각을 덮어쓰지 않는지 검증한다.
    @Test
    void repeatedCompleteRequestKeepsTheOriginalCompletionTime() {
        ActionPlan actionPlan = actionPlan();
        LocalDateTime firstCompletedAt = LocalDateTime.of(2026, 7, 30, 12, 0);

        actionPlan.complete(firstCompletedAt);
        actionPlan.complete(firstCompletedAt.plusHours(1));

        assertThat(actionPlan.getStatus()).isEqualTo(ActionPlanStatus.DONE);
        assertThat(actionPlan.getCompletedAt()).isEqualTo(firstCompletedAt);
    }

    // AI 생성 값과 최초 PENDING 상태를 가진 액션플랜 fixture를 만든다.
    private ActionPlan actionPlan() {
        return ActionPlan.from(
                mock(AnalysisInputSnapshot.class),
                mock(MatchAnalysisResult.class),
                mock(AiCallLog.class),
                CustomizedAnalysisGenerationResult.Task.builder()
                        .taskId("task-1")
                        .relatedRequirementId("requirement-1")
                        .matchLevel(ActionPlanMatchLevel.LOW)
                        .missingPoint("운영 경험이 부족함")
                        .suggestion("장애 대응 사례를 정리하기")
                        .build()
        );
    }
}
