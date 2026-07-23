package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContext;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseStatus;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.service.GuideContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

// JSON-03 컨텍스트를 분리해 JSON-01·02 단계 실행만 조정한다.
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final Set<UserDocumentType> CANDIDATE_DOCUMENT_TYPES = Set.of(
            UserDocumentType.RESUME,
            UserDocumentType.COVER_LETTER,
            UserDocumentType.PORTFOLIO,
            UserDocumentType.EXPERIENCE_NOTE
    );

    private final AnalysisCaseRepository analysisCaseRepository;
    private final AnalysisInputSnapshotRepository analysisInputSnapshotRepository;
    private final AnalysisCaseService analysisCaseService;
    private final InitialAnalysisStageExecutor initialAnalysisStageExecutor;
    private final GuideContextService guideContextService;
    private final CustomizedSynthesisStageExecutor customizedSynthesisStageExecutor;

    // JSON-03을 한 번 조회해 독립적인 JSON-01·02 실행 단계에 전달한다.
    public void runInitialAnalysis(Long userId, Long analysisCaseId) {
        AnalysisCase analysisCase = findOwnedCase(userId, analysisCaseId);
        validateRunnableStatus(analysisCase);
        AnalysisInputSnapshotContext context = analysisCaseService.getAnalysisContext(userId, analysisCaseId);

        List<AnalysisInputSnapshotContextSource> jobPostingSources = context.getSources().stream()
                .filter(source -> source.getDocumentType() == UserDocumentType.JOB_POSTING)
                .toList();
        List<AnalysisInputSnapshotContextSource> companyInfoSources = context.getSources().stream()
                .filter(source -> source.getDocumentType() == UserDocumentType.COMPANY_INFO)
                .toList();
        List<AnalysisInputSnapshotContextSource> candidateSources = context.getSources().stream()
                .filter(source -> CANDIDATE_DOCUMENT_TYPES.contains(source.getDocumentType()))
                .toList();

        if (jobPostingSources.size() != 1) {
            throw new CustomException(ErrorCode.ANALYSIS_CASE_JOB_POSTING_REQUIRED);
        }
        if (candidateSources.isEmpty()) {
            throw new CustomException(ErrorCode.ANALYSIS_CASE_USER_MATERIAL_REQUIRED);
        }

        initialAnalysisStageExecutor.execute(
                AiExecutionStage.JOB_POSTING_ANALYSIS,
                context,
                jobPostingSources,
                companyInfoSources
        );
        initialAnalysisStageExecutor.execute(
                AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS,
                context,
                candidateSources,
                List.of()
        );
    }

    // JSON-01·02와 JSON-04를 준비한 뒤 동일 snapshot의 JSON-05 실행만 조정한다.
    public void runCustomizedAnalysis(Long userId, Long analysisCaseId) {
        AnalysisCase analysisCase = findOwnedCase(userId, analysisCaseId);
        if (analysisCase.getStatus() != AnalysisCaseStatus.COMPLETED) {
            validateRunnableStatus(analysisCase);
            runInitialAnalysis(userId, analysisCaseId);
        }

        Long snapshotId = analysisInputSnapshotRepository
                .findByAnalysisCase_AnalysisCaseIdAndUser_UserId(analysisCaseId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND))
                .getSnapshotId();
        guideContextService.getOrCreateCustomizedSynthesisGuideContext(userId, snapshotId);
        customizedSynthesisStageExecutor.execute(snapshotId);
    }

    private AnalysisCase findOwnedCase(Long userId, Long analysisCaseId) {
        return analysisCaseRepository.findByAnalysisCaseIdAndUser_UserId(analysisCaseId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_CASE_NOT_FOUND));
    }

    private void validateRunnableStatus(AnalysisCase analysisCase) {
        if (analysisCase.getStatus() != AnalysisCaseStatus.INPUT_CONFIRMED
                && analysisCase.getStatus() != AnalysisCaseStatus.ANALYZING) {
            throw new CustomException(ErrorCode.ANALYSIS_CASE_NOT_READY);
        }
    }
}
