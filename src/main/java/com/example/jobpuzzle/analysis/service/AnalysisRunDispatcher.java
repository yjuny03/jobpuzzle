package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

/**
 * 분석 실행 요청을 접수만 하고, 실제 분석은 전용 스레드 풀에서 실행한다.
 *
 * 분석은 AI 호출 때문에 수십 초~수 분 걸리므로, HTTP 요청 스레드가 끝날 때까지 기다리지 않게 한다.
 * 진행 상황과 실패는 이미 DB(분석 상태, AI 호출 기록)에 남으므로 화면은 /status로 확인한다.
 */
@Slf4j
@Component
public class AnalysisRunDispatcher {

    private final AnalysisService analysisService;
    private final AnalysisCaseStatusTransitionService statusTransitionService;
    private final TaskExecutor analysisExecutor;

    public AnalysisRunDispatcher(AnalysisService analysisService,
                                 AnalysisCaseStatusTransitionService statusTransitionService,
                                 @Qualifier("analysisExecutor") TaskExecutor analysisExecutor) {
        this.analysisService = analysisService;
        this.statusTransitionService = statusTransitionService;
        this.analysisExecutor = analysisExecutor;
    }

    public void dispatch(Long userId, Long analysisCaseId) {
        // 소유권 획득과 사전 조건 검사는 요청 스레드에서 끝내, 실패 이유를 HTTP 응답으로 바로 돌려준다.
        if (!analysisService.startCustomizedAnalysis(userId, analysisCaseId)) {
            return; // 이미 분석 중이면 기존 실행을 그대로 둔다.
        }
        try {
            analysisExecutor.execute(() -> runInBackground(userId, analysisCaseId));
        } catch (TaskRejectedException rejected) {
            // 대기열이 가득 차 실행하지 못하면 ANALYZING으로 남지 않게 실패로 확정하고 다시 시도하도록 안내한다.
            statusTransitionService.failIfAnalyzing(userId, analysisCaseId);
            throw new CustomException(ErrorCode.ANALYSIS_QUEUE_FULL);
        }
    }

    private void runInBackground(Long userId, Long analysisCaseId) {
        try {
            analysisService.continueCustomizedAnalysis(userId, analysisCaseId);
        } catch (RuntimeException exception) {
            // 실패 상태와 AI 호출 기록은 서비스가 이미 저장했다. 여기서는 받을 HTTP 요청이 없으므로 로그만 남긴다.
            log.warn("background analysis failed. analysisCaseId={}, error={}", analysisCaseId, exception.toString());
        }
    }
}
