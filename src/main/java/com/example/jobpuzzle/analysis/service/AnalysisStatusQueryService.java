package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.log.*;
import com.example.jobpuzzle.analysis.dto.*;
import com.example.jobpuzzle.analysis.entity.*;
import com.example.jobpuzzle.analysis.repository.*;
import com.example.jobpuzzle.guide.entity.*;
import com.example.jobpuzzle.guide.repository.*;
import com.example.jobpuzzle.interview.entity.*;
import com.example.jobpuzzle.interview.repository.*;
import com.example.jobpuzzle.global.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisStatusQueryService {
    private final AnalysisCaseRepository cases;
    private final AnalysisInputSnapshotRepository snapshots;
    private final JobPostingAnalysisRepository jobs;
    private final CandidateMaterialAnalysisRepository candidates;
    private final GuideContextResultRepository guides;
    private final ReadinessResultRepository readiness;
    private final QuestionSetRepository sets;
    private final AiCallLogRepository logs;

    @Transactional(readOnly = true)
    public AnalysisStatusResponse getStatus(Long userId, Long caseId) {
        AnalysisCase c = cases.findByAnalysisCaseIdAndUser_UserId(caseId, userId).orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_CASE_NOT_FOUND));
        AnalysisInputSnapshot s = snapshots.findByAnalysisCase_AnalysisCaseIdAndUser_UserId(caseId, userId).orElse(null);
        if (s == null)
            return AnalysisStatusResponse.builder().analysisCaseId(caseId).analysisCaseStatus(c.getStatus().name()).jobPostingAnalysisStatus(AnalysisStageStatus.NOT_STARTED).candidateMaterialAnalysisStatus(AnalysisStageStatus.NOT_STARTED).guideContextStatus(AnalysisStageStatus.NOT_STARTED).customizedAnalysisStatus(AnalysisStageStatus.NOT_STARTED).build();
        Long id = s.getSnapshotId();
        var j = jobs.findBySnapshot_SnapshotId(id).orElse(null);
        var m = candidates.findBySnapshot_SnapshotId(id).orElse(null);
        var r = readiness.findBySnapshot_SnapshotId(id).orElse(null);
        var failure = logs.findFirstByInputReferenceTypeAndInputReferenceIdAndStatusOrderByAiCallLogIdDesc(AiInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(id), AiCallLogStatus.FAILED).orElse(null);
        return AnalysisStatusResponse.builder().analysisCaseId(caseId).snapshotId(id).analysisCaseStatus(c.getStatus().name()).jobPostingAnalysisStatus(stage(j == null ? null : j.getAiCallLog())).candidateMaterialAnalysisStatus(stage(m == null ? null : m.getAiCallLog())).guideContextStatus(guides.findByPurposeAndInputReferenceTypeAndInputReferenceId(GuideContextPurpose.CUSTOMIZED_SYNTHESIS, GuideContextInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(id)).isPresent() ? AnalysisStageStatus.SUCCEEDED : AnalysisStageStatus.NOT_STARTED).customizedAnalysisStatus(stage(r == null ? null : r.getAiCallLog())).readinessStatus(r == null ? null : r.getStatus().name()).canGenerateQuestions(r == null ? null : r.isCanGenerateQuestions()).questionSetAvailable(sets.existsBySnapshot_SnapshotIdAndInterviewMode(id, InterviewSessionMode.COMPANY_FIT)).latestFailureStage(failure == null ? null : failure.getExecutionStage().name()).latestFailureType(failure == null ? null : failure.getErrorType().name()).latestFailureMessage(failure == null ? null : failure.getErrorMessage()).build();
    }

    private AnalysisStageStatus stage(AiCallLog l) {
        if (l == null) return AnalysisStageStatus.NOT_STARTED;
        return switch (l.getStatus()) {
            case PENDING, RUNNING -> AnalysisStageStatus.RUNNING;
            case SUCCEEDED -> AnalysisStageStatus.SUCCEEDED;
            case FAILED -> AnalysisStageStatus.FAILED;
        };
    }
}
