package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.log.*;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRenderer;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.ai.service.AiClientService;
import com.example.jobpuzzle.ai.validation.AiResponseProcessor;
import com.example.jobpuzzle.ai.validation.CustomizedAnalysisResponseValidator;
import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.analysis.entity.*;
import com.example.jobpuzzle.analysis.repository.*;
import com.example.jobpuzzle.guide.entity.GuideContextResult;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import com.example.jobpuzzle.guide.repository.GuideContextChunkRepository;
import com.example.jobpuzzle.guide.repository.GuideContextResultRepository;
import com.example.jobpuzzle.interview.repository.InterviewQuestionRepository;
import com.example.jobpuzzle.interview.repository.QuestionSetRepository;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomizedSynthesisStageExecutorTest {
    private static final long SNAPSHOT_ID = 501L;

    @Mock private AnalysisInputSnapshotRepository snapshotRepository;
    @Mock private JobPostingAnalysisRepository jobPostingRepository;
    @Mock private CandidateMaterialAnalysisRepository candidateRepository;
    @Mock private GuideContextResultRepository guideContextRepository;
    @Mock private GuideContextChunkRepository guideContextChunkRepository;
    @Mock private ReadinessResultRepository readinessRepository;
    @Mock private MatchAnalysisResultRepository matchRepository;
    @Mock private ActionPlanRepository actionPlanRepository;
    @Mock private QuestionSetRepository questionSetRepository;
    @Mock private InterviewQuestionRepository questionRepository;
    @Mock private AiCallLogRepository aiCallLogRepository;
    @Mock private PromptTemplateRepository promptTemplateRepository;
    @Mock private AiClientService aiClientService;
    @Mock private PromptTemplateRenderer promptTemplateRenderer;
    @Mock private AiResponseProcessor aiResponseProcessor;
    @Mock private CustomizedAnalysisResponseValidator validator;
    @Mock private CustomizedAnalysisInputMapper inputMapper;
    @Mock private CustomizedSynthesisResultWriter resultWriter;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private AnalysisInputSnapshot snapshot;
    @Mock private JobPostingAnalysis jobPosting;
    @Mock private CandidateMaterialAnalysis candidate;

    private CustomizedSynthesisStageExecutor executor;
    private AiCallLog runningLog;

    @BeforeEach
    void setUp() {
        executor = new CustomizedSynthesisStageExecutor(snapshotRepository, jobPostingRepository, candidateRepository,
                guideContextRepository, guideContextChunkRepository, readinessRepository, matchRepository,
                actionPlanRepository, questionSetRepository, questionRepository, aiCallLogRepository,
                promptTemplateRepository, aiClientService, promptTemplateRenderer, aiResponseProcessor, validator,
                inputMapper, resultWriter, new ObjectMapper(), 600L, transactionManager);
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());

        JobCategory category = JobCategory.builder().mainCategory("IT").subCategory("BACKEND")
                .careerLevel(JobCategoryCareerLevel.NEW).build();
        AnalysisCase analysisCase = AnalysisCase.builder().user(null).jobCategory(category).build();
        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.ANALYZING);
        when(snapshot.getAnalysisCase()).thenReturn(analysisCase);
        when(snapshot.getJobCategory()).thenReturn(category);
        when(snapshotRepository.findWithLockBySnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(snapshot));
        when(jobPostingRepository.findBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(jobPosting));
        when(candidateRepository.findBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(candidate));
        when(readinessRepository.existsBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(false);
        when(matchRepository.existsBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(false);
        when(actionPlanRepository.existsBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(false);
        when(questionSetRepository.existsBySnapshot_SnapshotIdAndInterviewMode(anyLong(), any())).thenReturn(false);

        PromptTemplate initialPrompt = prompt("JSON-01", 1L);
        AiCallLog json01Log = succeededLog(initialPrompt, AiExecutionStage.JOB_POSTING_ANALYSIS);
        AiCallLog json02Log = succeededLog(initialPrompt, AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS);
        when(jobPosting.getAiCallLog()).thenReturn(json01Log);
        when(candidate.getAiCallLog()).thenReturn(json02Log);
        when(inputMapper.jobPosting(jobPosting)).thenReturn(JobPostingAnalysisResult.builder()
                .mainTasks(List.of()).requirements(List.of()).preferred(List.of()).companyValues(List.of())
                .coreCompetencies(List.of()).conflicts(List.of()).missingEvidence(List.of()).build());
        when(inputMapper.candidate(candidate)).thenReturn(CandidateMaterialAnalysisResult.builder()
                .availableDocumentTypes(List.of()).missingEvidence(List.of()).build());

        GuideContextResult guide = GuideContextResult.create(null, String.valueOf(SNAPSHOT_ID), category, null, GuideMatchType.NONE);
        ReflectionTestUtils.setField(guide, "guideContextResultId", 80L);
        when(guideContextRepository.findByPurposeAndInputReferenceTypeAndInputReferenceId(any(), any(), eq(String.valueOf(SNAPSHOT_ID))))
                .thenReturn(Optional.of(guide));
        when(guideContextChunkRepository.findByGuideContextResult_GuideContextResultIdOrderByDisplayOrderAsc(80L)).thenReturn(List.of());

        PromptTemplate synthesisPrompt = prompt("JSON-05", 2L);
        when(promptTemplateRepository.findFirstByTargetJsonAndIsActiveTrueOrderByPromptTemplateIdDesc("JSON-05"))
                .thenReturn(Optional.of(synthesisPrompt));
        when(aiClientService.getProvider()).thenReturn(AiProvider.MOCK);
        when(aiClientService.getModel()).thenReturn("mock-v1");
        when(aiCallLogRepository.findFirstByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndInputFingerprintOrderByAiCallLogIdDesc(
                any(), any(), anyString(), anyString())).thenReturn(Optional.empty());
        when(aiCallLogRepository.saveAndFlush(any(AiCallLog.class))).thenAnswer(invocation -> {
            runningLog = invocation.getArgument(0);
            ReflectionTestUtils.setField(runningLog, "aiCallLogId", 900L);
            return runningLog;
        });
        when(aiCallLogRepository.findById(900L)).thenAnswer(invocation -> Optional.ofNullable(runningLog));
    }

    @Test
    void providerFailureLeavesNoResultAndMarksOnlyTheClaimedLogFailed() {
        when(promptTemplateRenderer.renderCustomizedAnalysis(any(), anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn("rendered prompt");
        when(aiClientService.generateCustomizedAnalysis("rendered prompt"))
                .thenThrow(new IllegalStateException("provider unavailable"));

        executor.execute(SNAPSHOT_ID);

        verify(resultWriter, never()).write(anyLong(), anyLong(), any(), any());
        assertThat(runningLog.getStatus()).isEqualTo(AiCallLogStatus.FAILED);
        assertThat(runningLog.getErrorType()).isEqualTo(AiCallLogErrorType.PROVIDER_ERROR);
    }

    @Test
    void activeRunningLogBlocksAnotherProviderCall() {
        AiCallLog existing = succeededLog(prompt("JSON-05", 2L), AiExecutionStage.CUSTOMIZED_SYNTHESIS);
        existing.start();
        when(aiCallLogRepository.findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatusIn(
                eq(AiExecutionStage.CUSTOMIZED_SYNTHESIS), eq(AiInputReferenceType.ANALYSIS_SNAPSHOT),
                eq(String.valueOf(SNAPSHOT_ID)), any())).thenReturn(List.of(existing));

        executor.execute(SNAPSHOT_ID);

        verify(aiClientService, never()).generateCustomizedAnalysis(anyString());
        verify(resultWriter, never()).write(anyLong(), anyLong(), any(), any());
    }

    @Test
    void differentFingerprintRunningLogAlsoBlocksProviderCall() {
        when(aiCallLogRepository.findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatusIn(
                eq(AiExecutionStage.CUSTOMIZED_SYNTHESIS), eq(AiInputReferenceType.ANALYSIS_SNAPSHOT),
                eq(String.valueOf(SNAPSHOT_ID)), any())).thenReturn(List.of(runningLog("different-fingerprint", AiCallLogStatus.RUNNING)));

        executor.execute(SNAPSHOT_ID);

        verify(aiClientService, never()).generateCustomizedAnalysis(anyString());
        verify(aiCallLogRepository, never()).saveAndFlush(any(AiCallLog.class));
    }

    @Test
    void differentFingerprintPendingLogAlsoBlocksProviderCall() {
        AiCallLog pending = AiCallLog.pending(AiProvider.MOCK, "mock-v1", AiExecutionStage.CUSTOMIZED_SYNTHESIS,
                AiInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(SNAPSHOT_ID), "different-fingerprint", prompt("JSON-05", 2L), null, null);
        when(aiCallLogRepository.findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatusIn(
                eq(AiExecutionStage.CUSTOMIZED_SYNTHESIS), eq(AiInputReferenceType.ANALYSIS_SNAPSHOT),
                eq(String.valueOf(SNAPSHOT_ID)), any())).thenReturn(List.of(pending));

        executor.execute(SNAPSHOT_ID);

        verify(aiClientService, never()).generateCustomizedAnalysis(anyString());
        verify(aiCallLogRepository, never()).saveAndFlush(any(AiCallLog.class));
    }

    @Test
    void runningLogForAnotherSnapshotDoesNotBlockCurrentSnapshot() {
        prepareSuccessfulExecution();
        when(aiCallLogRepository.findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatusIn(
                eq(AiExecutionStage.CUSTOMIZED_SYNTHESIS), eq(AiInputReferenceType.ANALYSIS_SNAPSHOT),
                eq(String.valueOf(SNAPSHOT_ID)), any())).thenReturn(List.of());

        executor.execute(SNAPSHOT_ID);

        verify(aiClientService).generateCustomizedAnalysis("rendered prompt");
        verify(resultWriter).write(eq(SNAPSHOT_ID), eq(900L), any(), any());
    }

    @Test
    void staleDifferentFingerprintRunningLogFailsThenAllowsNewExecution() {
        AiCallLog stale = runningLog("different-fingerprint", AiCallLogStatus.RUNNING);
        ReflectionTestUtils.setField(stale, "startedAt", LocalDateTime.now().minusSeconds(601));
        when(aiCallLogRepository.findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatusIn(
                eq(AiExecutionStage.CUSTOMIZED_SYNTHESIS), eq(AiInputReferenceType.ANALYSIS_SNAPSHOT),
                eq(String.valueOf(SNAPSHOT_ID)), any())).thenReturn(List.of(stale));
        prepareSuccessfulExecution();

        executor.execute(SNAPSHOT_ID);

        assertThat(stale.getStatus()).isEqualTo(AiCallLogStatus.FAILED);
        assertThat(stale.getErrorType()).isEqualTo(AiCallLogErrorType.STALE_RUNNING);
        verify(aiClientService).generateCustomizedAnalysis("rendered prompt");
    }

    @Test
    void multipleActiveLogsAreRejectedAsIntegrityConflict() {
        when(aiCallLogRepository.findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatusIn(
                eq(AiExecutionStage.CUSTOMIZED_SYNTHESIS), eq(AiInputReferenceType.ANALYSIS_SNAPSHOT),
                eq(String.valueOf(SNAPSHOT_ID)), any())).thenReturn(List.of(
                runningLog("one", AiCallLogStatus.RUNNING), runningLog("two", AiCallLogStatus.PENDING)));

        assertThatThrownBy(() -> executor.execute(SNAPSHOT_ID))
                .isInstanceOf(com.example.jobpuzzle.global.error.CustomException.class)
                .extracting(error -> ((com.example.jobpuzzle.global.error.CustomException) error).getErrorCode())
                .isEqualTo(com.example.jobpuzzle.global.error.ErrorCode.JSON05_RESULT_INTEGRITY_CONFLICT);
        verify(aiClientService, never()).generateCustomizedAnalysis(anyString());
    }

    @Test
    void succeededLogWithoutResultIsIntegrityConflictAndDoesNotCreateNewExecution() {
        AiCallLog succeeded = runningLog("old", AiCallLogStatus.RUNNING);
        succeeded.succeed();
        when(aiCallLogRepository.findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatus(
                eq(AiExecutionStage.CUSTOMIZED_SYNTHESIS), eq(AiInputReferenceType.ANALYSIS_SNAPSHOT),
                eq(String.valueOf(SNAPSHOT_ID)), eq(AiCallLogStatus.SUCCEEDED))).thenReturn(List.of(succeeded));

        assertThatThrownBy(() -> executor.execute(SNAPSHOT_ID))
                .isInstanceOf(com.example.jobpuzzle.global.error.CustomException.class)
                .extracting(error -> ((com.example.jobpuzzle.global.error.CustomException) error).getErrorCode())
                .isEqualTo(com.example.jobpuzzle.global.error.ErrorCode.JSON05_RESULT_INTEGRITY_CONFLICT);
        assertThat(succeeded.getStatus()).isEqualTo(AiCallLogStatus.SUCCEEDED);
        verify(aiClientService, never()).generateCustomizedAnalysis(anyString());
        verify(aiCallLogRepository, never()).saveAndFlush(any(AiCallLog.class));
    }

    @Test
    void readinessOnlyResultIsRejectedAsIntegrityConflict() {
        AiCallLog successLog = customizedSucceededLog(71L);
        ReadinessResult readiness = mock(ReadinessResult.class);
        when(readiness.getAiCallLog()).thenReturn(successLog);
        when(readiness.isCanGenerateQuestions()).thenReturn(false);
        when(readinessRepository.existsBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(true);
        when(readinessRepository.findBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(readiness));
        when(inputMapper.jobPosting(jobPosting)).thenReturn(postingWithRequirement());
        when(matchRepository.findBySnapshot_SnapshotIdOrderByMatchIdAsc(SNAPSHOT_ID)).thenReturn(List.of());

        assertIntegrityConflict();
    }

    @Test
    void questionGenerationReadyResultWithoutQuestionSetIsRejected() {
        AiCallLog successLog = customizedSucceededLog(72L);
        ReadinessResult readiness = mock(ReadinessResult.class);
        when(readiness.getAiCallLog()).thenReturn(successLog);
        when(readiness.isCanGenerateQuestions()).thenReturn(true);
        when(readinessRepository.existsBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(true);
        when(readinessRepository.findBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(readiness));
        when(matchRepository.findBySnapshot_SnapshotIdOrderByMatchIdAsc(SNAPSHOT_ID)).thenReturn(List.of());
        when(questionSetRepository.findBySnapshot_SnapshotIdAndInterviewMode(anyLong(), any())).thenReturn(Optional.empty());

        assertIntegrityConflict();
    }

    @Test
    void questionSetForQuestionGenerationDisabledResultIsRejected() {
        AiCallLog successLog = customizedSucceededLog(73L);
        ReadinessResult readiness = mock(ReadinessResult.class);
        when(readiness.getAiCallLog()).thenReturn(successLog);
        when(readiness.isCanGenerateQuestions()).thenReturn(false);
        when(readinessRepository.existsBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(true);
        when(readinessRepository.findBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(readiness));
        when(matchRepository.findBySnapshot_SnapshotIdOrderByMatchIdAsc(SNAPSHOT_ID)).thenReturn(List.of());
        when(questionSetRepository.existsBySnapshot_SnapshotIdAndInterviewMode(anyLong(), any())).thenReturn(true);

        assertIntegrityConflict();
    }

    @Test
    void resultEntitiesWithDifferentAiCallLogsAreRejected() {
        AiCallLog readinessLog = customizedSucceededLog(74L);
        AiCallLog actionLog = customizedSucceededLog(75L);
        ReadinessResult readiness = mock(ReadinessResult.class);
        ActionPlan action = mock(ActionPlan.class);
        when(readiness.getAiCallLog()).thenReturn(readinessLog);
        when(readiness.isCanGenerateQuestions()).thenReturn(false);
        when(action.getAiCallLog()).thenReturn(actionLog);
        when(readinessRepository.existsBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(true);
        when(readinessRepository.findBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(readiness));
        when(matchRepository.findBySnapshot_SnapshotIdOrderByMatchIdAsc(SNAPSHOT_ID)).thenReturn(List.of());
        when(actionPlanRepository.findBySnapshot_SnapshotIdOrderByActionPlanIdAsc(SNAPSHOT_ID)).thenReturn(List.of(action));

        assertIntegrityConflict();
    }

    @Test
    void writerFailureUsesPersistenceErrorWithoutLeakingExceptionMessage() {
        prepareSuccessfulExecution();
        doThrow(new IllegalStateException("resume evidenceText: private candidate material"))
                .when(resultWriter).write(anyLong(), anyLong(), any(), any());

        executor.execute(SNAPSHOT_ID);

        assertThat(runningLog.getStatus()).isEqualTo(AiCallLogStatus.FAILED);
        assertThat(runningLog.getErrorType()).isEqualTo(AiCallLogErrorType.RESULT_PERSIST_FAILED);
        assertThat(runningLog.getErrorMessage()).isEqualTo("Customized analysis result persistence failed");
        assertThat(runningLog.getErrorMessage()).doesNotContain("private candidate material");
    }

    private void prepareSuccessfulExecution() {
        when(promptTemplateRenderer.renderCustomizedAnalysis(any(), anyString(), anyString(), anyString(), any(), any(), any()))
                .thenReturn("rendered prompt");
        when(aiClientService.generateCustomizedAnalysis("rendered prompt")).thenReturn("{}");
        when(aiResponseProcessor.parseCustomizedAnalysis("{}")).thenReturn(mock(CustomizedAnalysisGenerationResult.class));
    }

    private void assertIntegrityConflict() {
        assertThatThrownBy(() -> executor.execute(SNAPSHOT_ID))
                .isInstanceOf(com.example.jobpuzzle.global.error.CustomException.class)
                .extracting(error -> ((com.example.jobpuzzle.global.error.CustomException) error).getErrorCode())
                .isEqualTo(com.example.jobpuzzle.global.error.ErrorCode.JSON05_RESULT_INTEGRITY_CONFLICT);
        verify(aiClientService, never()).generateCustomizedAnalysis(anyString());
    }

    private JobPostingAnalysisResult postingWithRequirement() {
        return JobPostingAnalysisResult.builder().mainTasks(List.of()).requirements(List.of(
                JobPostingAnalysisResult.Requirement.builder().requirementId("required-1").text("Java").sourceRefs(List.of()).build()))
                .preferred(List.of()).companyValues(List.of()).coreCompetencies(List.of()).conflicts(List.of()).missingEvidence(List.of()).build();
    }

    private PromptTemplate prompt(String targetJson, Long id) {
        PromptTemplate prompt = PromptTemplate.builder().promptCode(targetJson).name(targetJson).version("v1")
                .targetJson(targetJson).templateText("template").isActive(true).build();
        ReflectionTestUtils.setField(prompt, "promptTemplateId", id);
        return prompt;
    }

    private AiCallLog succeededLog(PromptTemplate prompt, AiExecutionStage stage) {
        AiCallLog log = AiCallLog.pending(AiProvider.MOCK, "mock-v1", stage,
                AiInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(SNAPSHOT_ID), stage.name(), prompt, null, null);
        log.start();
        log.succeed();
        return log;
    }

    private AiCallLog runningLog(String fingerprint, AiCallLogStatus status) {
        AiCallLog log = AiCallLog.pending(AiProvider.MOCK, "mock-v1", AiExecutionStage.CUSTOMIZED_SYNTHESIS,
                AiInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(SNAPSHOT_ID), fingerprint, prompt("JSON-05", 2L), null, null);
        if (status == AiCallLogStatus.RUNNING) {
            log.start();
        }
        return log;
    }

    private AiCallLog customizedSucceededLog(Long id) {
        AiCallLog log = runningLog("result-" + id, AiCallLogStatus.RUNNING);
        log.succeed();
        ReflectionTestUtils.setField(log, "aiCallLogId", id);
        return log;
    }
}
