package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseStatus;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 분석 파이프라인 상태 전이를 짧은 독립 트랜잭션으로 직렬화한다. */
@Service
@RequiredArgsConstructor
public class AnalysisCaseStatusTransitionService {
    private final AnalysisCaseRepository analysisCaseRepository;

    // 최초 실행·재시도만 소유권을 얻고 이미 실행 중인 동시 요청은 시작하지 않는다.
    @Transactional
    public boolean startOrRestart(Long userId, Long analysisCaseId) {
        AnalysisCase analysisCase = findOwnedCaseWithLock(userId, analysisCaseId);
        if (analysisCase.getStatus() == AnalysisCaseStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ANALYSIS_CASE_NOT_READY);
        }
        if (analysisCase.getStatus() == AnalysisCaseStatus.ANALYZING) {
            return false;
        }
        analysisCase.startOrRestartAnalysis();
        return true;
    }

    // 원래 실행 트랜잭션과 무관하게 ANALYZING case만 FAILED로 확정한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failIfAnalyzing(Long userId, Long analysisCaseId) {
        AnalysisCase analysisCase = findOwnedCaseWithLock(userId, analysisCaseId);
        if (analysisCase.getStatus() == AnalysisCaseStatus.ANALYZING) {
            analysisCase.failAnalysis();
        }
    }

    private AnalysisCase findOwnedCaseWithLock(Long userId, Long analysisCaseId) {
        return analysisCaseRepository.findWithLockByAnalysisCaseIdAndUser_UserId(analysisCaseId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_CASE_NOT_FOUND));
    }
}
