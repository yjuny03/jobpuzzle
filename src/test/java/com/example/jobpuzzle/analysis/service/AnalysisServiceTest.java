package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContext;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseStatus;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.service.GuideContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalysisServiceTest {

    private static final long USER_ID = 1L;
    private static final long ANALYSIS_CASE_ID = 10L;

    @Mock
    private AnalysisCaseRepository analysisCaseRepository;
    @Mock
    private AnalysisCaseService analysisCaseService;
    @Mock
    private InitialAnalysisStageExecutor initialAnalysisStageExecutor;
    @Mock
    private AnalysisInputSnapshotRepository analysisInputSnapshotRepository;
    @Mock
    private GuideContextService guideContextService;
    @Mock
    private CustomizedSynthesisStageExecutor customizedSynthesisStageExecutor;
    @Mock
    private AnalysisInputSnapshotContext context;

    @InjectMocks
    private AnalysisService analysisService;

    private AnalysisCase analysisCase;
    private AnalysisInputSnapshotContextSource jobPosting;
    private AnalysisInputSnapshotContextSource companyInfo;
    private AnalysisInputSnapshotContextSource resume;

    @BeforeEach
    void setUp() {
        analysisCase = AnalysisCase.builder().user(null).jobCategory(null).build();
        ReflectionTestUtils.setField(analysisCase, "analysisCaseId", ANALYSIS_CASE_ID);
        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.INPUT_CONFIRMED);
        when(analysisCaseRepository.findByAnalysisCaseIdAndUser_UserId(ANALYSIS_CASE_ID, USER_ID))
                .thenReturn(Optional.of(analysisCase));
        when(analysisCaseService.getAnalysisContext(USER_ID, ANALYSIS_CASE_ID)).thenReturn(context);

        jobPosting = source(UserDocumentType.JOB_POSTING, "공고", "[SOURCE job] 공고 본문");
        companyInfo = source(UserDocumentType.COMPANY_INFO, "회사", "[SOURCE company] 회사 본문");
        resume = source(UserDocumentType.RESUME, "이력서", "[SOURCE resume] 이력서 본문");
        when(context.getSources()).thenReturn(List.of(jobPosting, companyInfo, resume));
    }

    @Test
    @DisplayName("JSON-03을 한 번 조회하고 JSON-01·02 독립 실행 단계로 자료를 분리한다")
    void coordinatesSeparatedInitialAnalysisStages() {
        analysisService.runInitialAnalysis(USER_ID, ANALYSIS_CASE_ID);

        ArgumentCaptor<List<AnalysisInputSnapshotContextSource>> primaryCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<AnalysisInputSnapshotContextSource>> supplementaryCaptor = ArgumentCaptor.forClass(List.class);
        verify(initialAnalysisStageExecutor).execute(
                eq(AiExecutionStage.JOB_POSTING_ANALYSIS), eq(context), primaryCaptor.capture(), supplementaryCaptor.capture()
        );
        assertThat(primaryCaptor.getAllValues().get(0)).containsExactly(jobPosting);
        assertThat(supplementaryCaptor.getAllValues().get(0)).containsExactly(companyInfo);
        verify(initialAnalysisStageExecutor).execute(
                eq(AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS), eq(context), eq(List.of(resume)), eq(List.of())
        );
        verify(analysisCaseService).getAnalysisContext(USER_ID, ANALYSIS_CASE_ID);
    }

    @Test
    @DisplayName("다른 사용자의 분석 작업은 실행 단계에 전달하지 않는다")
    void rejectsAnotherUsersAnalysisCase() {
        when(analysisCaseRepository.findByAnalysisCaseIdAndUser_UserId(ANALYSIS_CASE_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.runInitialAnalysis(USER_ID, ANALYSIS_CASE_ID))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_CASE_NOT_FOUND);
        verify(analysisCaseService, never()).getAnalysisContext(any(), any());
        verify(initialAnalysisStageExecutor, never()).execute(any(), any(), any(), any());
    }

    @Test
    @DisplayName("INPUT_CONFIRMED 또는 ANALYZING이 아닌 작업은 실행할 수 없다")
    void rejectsAnalysisCaseOutsideRunnableStatus() {
        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.DRAFT);

        assertThatThrownBy(() -> analysisService.runInitialAnalysis(USER_ID, ANALYSIS_CASE_ID))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_CASE_NOT_READY);
        verify(analysisCaseService, never()).getAnalysisContext(any(), any());
    }

    @Test
    @DisplayName("맞춤 분석은 초기 분석 후 같은 snapshot의 가이드와 JSON-05 실행기를 순서대로 사용한다")
    void coordinatesCustomizedAnalysisFromTheOwnedSnapshot() {
        AnalysisInputSnapshot snapshot = org.mockito.Mockito.mock(AnalysisInputSnapshot.class);
        when(snapshot.getSnapshotId()).thenReturn(200L);
        when(analysisInputSnapshotRepository.findByAnalysisCase_AnalysisCaseIdAndUser_UserId(ANALYSIS_CASE_ID, USER_ID))
                .thenReturn(Optional.of(snapshot));

        analysisService.runCustomizedAnalysis(USER_ID, ANALYSIS_CASE_ID);

        verify(guideContextService).getOrCreateCustomizedSynthesisGuideContext(USER_ID, 200L);
        verify(customizedSynthesisStageExecutor).execute(200L);
    }

    private AnalysisInputSnapshotContextSource source(UserDocumentType type, String displayName, String analysisText) {
        AnalysisInputSnapshotContextSource source = org.mockito.Mockito.mock(AnalysisInputSnapshotContextSource.class);
        when(source.getDocumentType()).thenReturn(type);
        when(source.getDisplayName()).thenReturn(displayName);
        when(source.getAnalysisText()).thenReturn(analysisText);
        return source;
    }
}
