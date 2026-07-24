package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.log.*;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRenderer;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.ai.service.AiClientService;
import com.example.jobpuzzle.ai.validation.*;
import com.example.jobpuzzle.analysis.entity.*;
import com.example.jobpuzzle.analysis.repository.*;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.GuideContextResultDto;
import com.example.jobpuzzle.guide.entity.GuideContextInputReferenceType;
import com.example.jobpuzzle.guide.entity.GuideContextPurpose;
import com.example.jobpuzzle.guide.entity.GuideContextResult;
import com.example.jobpuzzle.guide.repository.GuideContextChunkRepository;
import com.example.jobpuzzle.guide.repository.GuideContextResultRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

// JSON-05 실행 선점·AI 호출·검증·실패 로그 처리를 저장기와 분리해 조정한다.
@Service
public class CustomizedSynthesisStageExecutor {
    private final AnalysisInputSnapshotRepository snapshotRepository;
    private final JobPostingAnalysisRepository jobPostingRepository;
    private final CandidateMaterialAnalysisRepository candidateRepository;
    private final GuideContextResultRepository guideContextRepository;
    private final GuideContextChunkRepository guideContextChunkRepository;
    private final ReadinessResultRepository readinessRepository;
    private final MatchAnalysisResultRepository matchRepository;
    private final ActionPlanRepository actionPlanRepository;
    private final com.example.jobpuzzle.interview.repository.QuestionSetRepository questionSetRepository;
    private final com.example.jobpuzzle.interview.repository.InterviewQuestionRepository questionRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AiClientService aiClientService;
    private final PromptTemplateRenderer promptTemplateRenderer;
    private final AiResponseProcessor aiResponseProcessor;
    private final CustomizedAnalysisResponseValidator responseValidator;
    private final CustomizedAnalysisInputMapper inputMapper;
    private final CustomizedSynthesisResultWriter resultWriter;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final long runningTimeoutSeconds;

    public CustomizedSynthesisStageExecutor(AnalysisInputSnapshotRepository snapshotRepository, JobPostingAnalysisRepository jobPostingRepository,
                                            CandidateMaterialAnalysisRepository candidateRepository, GuideContextResultRepository guideContextRepository,
                                            GuideContextChunkRepository guideContextChunkRepository, ReadinessResultRepository readinessRepository,
                                            MatchAnalysisResultRepository matchRepository, ActionPlanRepository actionPlanRepository,
                                            com.example.jobpuzzle.interview.repository.QuestionSetRepository questionSetRepository,
                                            com.example.jobpuzzle.interview.repository.InterviewQuestionRepository questionRepository,
                                            AiCallLogRepository aiCallLogRepository, PromptTemplateRepository promptTemplateRepository,
                                            AiClientService aiClientService, PromptTemplateRenderer promptTemplateRenderer,
                                            AiResponseProcessor aiResponseProcessor, CustomizedAnalysisResponseValidator responseValidator,
                                            CustomizedAnalysisInputMapper inputMapper, CustomizedSynthesisResultWriter resultWriter, ObjectMapper objectMapper,
                                            @Value("${app.ai.running-timeout-seconds}") long runningTimeoutSeconds,
                                            PlatformTransactionManager transactionManager) {
        this.snapshotRepository = snapshotRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.candidateRepository = candidateRepository;
        this.guideContextRepository = guideContextRepository;
        this.guideContextChunkRepository = guideContextChunkRepository;
        this.readinessRepository = readinessRepository;
        this.matchRepository = matchRepository;
        this.actionPlanRepository = actionPlanRepository;
        this.questionSetRepository = questionSetRepository;
        this.questionRepository = questionRepository;
        this.aiCallLogRepository = aiCallLogRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.aiClientService = aiClientService;
        this.promptTemplateRenderer = promptTemplateRenderer;
        this.aiResponseProcessor = aiResponseProcessor;
        this.responseValidator = responseValidator;
        this.inputMapper = inputMapper;
        this.resultWriter = resultWriter;
        this.objectMapper = objectMapper;
        this.runningTimeoutSeconds = runningTimeoutSeconds;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // 선점은 짧게 끝내고 Provider 호출과 검증은 DB 락 밖에서 수행한다.
    public void execute(Long snapshotId) {
        Lease lease = acquire(snapshotId);
        if (lease == null) return;
        try {
            String prompt = promptTemplateRenderer.renderCustomizedAnalysis(lease.promptTemplate(), lease.mainCategory(), lease.subCategory(),
                    lease.careerLevel(), lease.jobPosting(), lease.candidate(), lease.guideDto());
            executeWithPrompt(lease, prompt);
        } catch (RuntimeException exception) {
            recordFailure(lease.aiCallLogId(), errorType(exception), exception);
        }
    }

    // Provider 실패는 파싱·검증·저장 실패와 분리해 안전한 오류 유형으로 기록한다.
    private void executeWithPrompt(Lease lease, String prompt) {
        String rawResponse;
        try {
            rawResponse = aiClientService.generateCustomizedAnalysis(prompt);
        } catch (RuntimeException exception) {
            recordFailure(lease.aiCallLogId(), AiCallLogErrorType.PROVIDER_ERROR, exception);
            return;
        }
        try {
            CustomizedAnalysisGenerationResult result = aiResponseProcessor.parseCustomizedAnalysis(rawResponse);
            responseValidator.validate(new CustomizedAnalysisValidationContext(lease.jobPosting(), lease.candidate(), lease.guideDto()), result);
            persistResult(lease, result);
        } catch (RuntimeException exception) {
            recordFailure(lease.aiCallLogId(), errorType(exception), exception);
        }
    }

    // 검증 완료 결과의 DB 저장 실패는 Provider 오류와 구분한다.
    private void persistResult(Lease lease, CustomizedAnalysisGenerationResult result) {
        try {
            resultWriter.write(lease.snapshotId(), lease.aiCallLogId(), lease.guideContext(), result);
        } catch (RuntimeException exception) {
            recordFailure(lease.aiCallLogId(), AiCallLogErrorType.RESULT_PERSIST_FAILED, exception);
        }
    }

    // snapshot 잠금 안에서 기존 결과 무결성·RUNNING·stale을 판정하고 새 로그를 선점한다.
    private Lease acquire(Long snapshotId) {
        return transactionTemplate.execute(status -> {
            AnalysisInputSnapshot snapshot = lockedSnapshot(snapshotId);
            if (hasAnyJson05Result(snapshotId)) {
                if (isValidCompletedResult(snapshotId)) return null;
                throw integrityConflict();
            }
            rejectSucceededLogWithoutResult(snapshotId);
            if (hasActiveExecution(snapshotId)) return null;
            if (snapshot.getAnalysisCase().getStatus() != AnalysisCaseStatus.ANALYZING)
                throw new CustomException(ErrorCode.ANALYSIS_CASE_NOT_READY);
            JobPostingAnalysis jobPosting = jobPostingRepository.findBySnapshot_SnapshotId(snapshotId).orElseThrow(() -> new IllegalStateException("JSON-01 result is missing"));
            CandidateMaterialAnalysis candidate = candidateRepository.findBySnapshot_SnapshotId(snapshotId).orElseThrow(() -> new IllegalStateException("JSON-02 result is missing"));
            requireSucceeded(jobPosting.getAiCallLog(), "JSON-01");
            requireSucceeded(candidate.getAiCallLog(), "JSON-02");
            GuideContextResult guide = guideContextRepository.findByPurposeAndInputReferenceTypeAndInputReferenceId(
                            GuideContextPurpose.CUSTOMIZED_SYNTHESIS, GuideContextInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(snapshotId))
                    .orElseThrow(() -> new IllegalStateException("JSON-04 result is missing"));
            PromptTemplate template = promptTemplateRepository.findFirstByTargetJsonAndIsActiveTrueOrderByPromptTemplateIdDesc("JSON-05")
                    .orElseThrow(() -> new CustomException(ErrorCode.AI_PROMPT_TEMPLATE_NOT_FOUND));
            JobPostingAnalysisResult jobDto = inputMapper.jobPosting(jobPosting);
            CandidateMaterialAnalysisResult candidateDto = inputMapper.candidate(candidate);
            GuideContextResultDto guideDto = GuideContextResultDto.from(guide, guideContextChunkRepository.findByGuideContextResult_GuideContextResultIdOrderByDisplayOrderAsc(guide.getGuideContextResultId()));
            AiProvider provider = aiClientService.getProvider();
            String model = aiClientService.getModel();
            String fingerprint = fingerprint(provider, model, template, guide, jobDto, candidateDto, guideDto);
            AiCallLog latest = aiCallLogRepository.findFirstByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndInputFingerprintOrderByAiCallLogIdDesc(
                    AiExecutionStage.CUSTOMIZED_SYNTHESIS, AiInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(snapshotId), fingerprint).orElse(null);
            if (latest != null && stale(latest))
                latest.fail(AiCallLogErrorType.STALE_RUNNING, "AI execution timed out");
            else if (latest != null && (latest.getStatus() == AiCallLogStatus.RUNNING || latest.getStatus() == AiCallLogStatus.PENDING || latest.getStatus() == AiCallLogStatus.SUCCEEDED))
                return null;
            AiCallLog log = AiCallLog.pending(provider, model, AiExecutionStage.CUSTOMIZED_SYNTHESIS, AiInputReferenceType.ANALYSIS_SNAPSHOT,
                    String.valueOf(snapshotId), fingerprint, template, guide.getGuide(), latest != null && latest.getStatus() == AiCallLogStatus.FAILED ? latest : null);
            log.start();
            aiCallLogRepository.saveAndFlush(log);
            return new Lease(snapshotId, log.getAiCallLogId(), template, jobDto, candidateDto, guide, guideDto,
                    snapshot.getJobCategory().getMainCategory(), snapshot.getJobCategory().getSubCategory(), snapshot.getJobCategory().getCareerLevel().name());
        });
    }

    private void recordFailure(Long logId, AiCallLogErrorType errorType, RuntimeException exception) {
        transactionTemplate.executeWithoutResult(status -> aiCallLogRepository.findById(logId).ifPresent(log -> {
            if (log.getStatus() == AiCallLogStatus.RUNNING) log.fail(errorType, safeMessage(errorType, exception));
        }));
    }

    private AnalysisInputSnapshot lockedSnapshot(Long snapshotId) {
        return snapshotRepository.findWithLockBySnapshotId(snapshotId).orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));
    }

    private void requireSucceeded(AiCallLog log, String stage) {
        if (log == null || log.getStatus() != AiCallLogStatus.SUCCEEDED || !Boolean.TRUE.equals(log.getValid()))
            throw new IllegalStateException(stage + " is not succeeded");
    }

    private boolean hasAnyJson05Result(Long id) {
        return readinessRepository.existsBySnapshot_SnapshotId(id) || matchRepository.existsBySnapshot_SnapshotId(id) || actionPlanRepository.existsBySnapshot_SnapshotId(id) || questionSetRepository.existsBySnapshot_SnapshotIdAndInterviewMode(id, com.example.jobpuzzle.interview.entity.InterviewSessionMode.COMPANY_FIT);
    }

    // fingerprint와 무관하게 같은 snapshot의 활성 실행은 하나만 허용한다.
    private boolean hasActiveExecution(Long snapshotId) {
        List<AiCallLog> activeLogs = aiCallLogRepository.findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatusIn(
                AiExecutionStage.CUSTOMIZED_SYNTHESIS, AiInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(snapshotId),
                List.of(AiCallLogStatus.PENDING, AiCallLogStatus.RUNNING));
        if (activeLogs.size() > 1) throw integrityConflict();
        if (activeLogs.size() == 1) {
            AiCallLog active = activeLogs.get(0);
            if (stale(active)) {
                active.fail(AiCallLogErrorType.STALE_RUNNING, "AI execution timed out");
                return false;
            }
            return true;
        }
        return false;
    }

    private void rejectSucceededLogWithoutResult(Long snapshotId) {
        List<AiCallLog> succeededLogs = aiCallLogRepository.findByExecutionStageAndInputReferenceTypeAndInputReferenceIdAndStatus(
                AiExecutionStage.CUSTOMIZED_SYNTHESIS, AiInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(snapshotId), AiCallLogStatus.SUCCEEDED);
        if (!succeededLogs.isEmpty()) throw integrityConflict();
    }

    private boolean isValidCompletedResult(Long id) {
        ReadinessResult readiness = readinessRepository.findBySnapshot_SnapshotId(id).orElse(null);
        if (readiness == null || !isSucceededCustomizedLog(readiness.getAiCallLog())) return false;
        AiCallLog resultLog = readiness.getAiCallLog();
        List<MatchAnalysisResult> matches = matchRepository.findBySnapshot_SnapshotIdOrderByMatchIdAsc(id);
        if (!hasExpectedMatches(id, matches)) return false;
        if (matches.stream().anyMatch(value -> !sameLog(resultLog, value.getAiCallLog()))) return false;
        if (readiness.isCanGenerateQuestions()) {
            var set = questionSetRepository.findBySnapshot_SnapshotIdAndInterviewMode(id, com.example.jobpuzzle.interview.entity.InterviewSessionMode.COMPANY_FIT).orElse(null);
            var questions = set == null ? List.<com.example.jobpuzzle.interview.entity.InterviewQuestion>of()
                    : questionRepository.findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(set.getQuestionSetId());
            if (set == null || !sameLog(resultLog, set.getAiCallLog()) || questions.isEmpty() || questions.size() > 10
                    || questions.stream().anyMatch(question -> question.getReviewStatus() != com.example.jobpuzzle.interview.entity.InterviewQuestionReviewStatus.PASS))
                return false;
        } else if (questionSetRepository.existsBySnapshot_SnapshotIdAndInterviewMode(id, com.example.jobpuzzle.interview.entity.InterviewSessionMode.COMPANY_FIT))
            return false;
        List<ActionPlan> actions = actionPlanRepository.findBySnapshot_SnapshotIdOrderByActionPlanIdAsc(id);
        return actions.stream().allMatch(value -> sameLog(resultLog, value.getAiCallLog()))
                && matches.stream().filter(value -> value.getMatchLevel() != MatchAnalysisResultMatchLevel.HIGH)
                .allMatch(match -> actions.stream().anyMatch(action -> action.getMatchAnalysisResult() != null
                        && match.getMatchId() != null && match.getMatchId().equals(action.getMatchAnalysisResult().getMatchId())));
    }

    private boolean hasExpectedMatches(Long snapshotId, List<MatchAnalysisResult> matches) {
        JobPostingAnalysis posting = jobPostingRepository.findBySnapshot_SnapshotId(snapshotId).orElse(null);
        if (posting == null) return false;
        JobPostingAnalysisResult result = inputMapper.jobPosting(posting);
        java.util.Set<String> expected = new java.util.HashSet<>();
        if (result.getRequirements() != null)
            result.getRequirements().forEach(value -> expected.add(value.getRequirementId()));
        if (result.getPreferred() != null)
            result.getPreferred().forEach(value -> expected.add(value.getRequirementId()));
        java.util.Set<String> actual = matches.stream().map(MatchAnalysisResult::getRequirementId).collect(java.util.stream.Collectors.toSet());
        return expected.equals(actual) && expected.size() == matches.size();
    }

    private boolean isSucceededCustomizedLog(AiCallLog log) {
        return log != null && log.getStatus() == AiCallLogStatus.SUCCEEDED && log.getExecutionStage() == AiExecutionStage.CUSTOMIZED_SYNTHESIS;
    }

    private boolean sameLog(AiCallLog expected, AiCallLog actual) {
        return isSucceededCustomizedLog(actual) && expected.getAiCallLogId() != null && expected.getAiCallLogId().equals(actual.getAiCallLogId());
    }

    private boolean stale(AiCallLog log) {
        return (log.getStatus() == AiCallLogStatus.RUNNING || log.getStatus() == AiCallLogStatus.PENDING)
                && log.getStartedAt() != null && log.getStartedAt().plusSeconds(runningTimeoutSeconds).isBefore(LocalDateTime.now());
    }

    private String fingerprint(AiProvider provider, String model, PromptTemplate template, GuideContextResult guide, Object job, Object candidate, Object guideDto) {
        try {
            String value = AiExecutionStage.CUSTOMIZED_SYNTHESIS + "|" + provider + "|" + model + "|" + template.getPromptTemplateId() + "|" + template.getVersion() + "|" + (guide.getGuide() == null ? "null" : guide.getGuide().getGuideId()) + "|" + guide.getGuideVersion() + "|" + objectMapper.writeValueAsString(job) + "|" + objectMapper.writeValueAsString(candidate) + "|" + objectMapper.writeValueAsString(guideDto);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("cannot create JSON-05 fingerprint", exception);
        }
    }

    private AiCallLogErrorType errorType(RuntimeException exception) {
        return exception instanceof AiProcessingException value ? value.getErrorType() : AiCallLogErrorType.PROVIDER_ERROR;
    }

    private String safeMessage(AiCallLogErrorType errorType, RuntimeException exception) {
        return switch (errorType) {
            case PROVIDER_ERROR -> "AI provider request failed";
            case RESULT_PERSIST_FAILED -> "Customized analysis result persistence failed";
            case PROMPT_RENDER_FAILED -> "Prompt rendering failed";
            case RESPONSE_PARSE_FAILED -> "AI response parsing failed";
            case RESPONSE_VALIDATION_FAILED -> "AI response validation failed";
            case SOURCE_REFERENCE_INVALID -> "AI source reference validation failed";
            case STALE_RUNNING -> "AI execution timed out";
            default -> "Customized analysis execution failed";
        };
    }

    private CustomException integrityConflict() {
        return new CustomException(ErrorCode.JSON05_RESULT_INTEGRITY_CONFLICT);
    }

    private record Lease(Long snapshotId, Long aiCallLogId, PromptTemplate promptTemplate,
                         JobPostingAnalysisResult jobPosting, CandidateMaterialAnalysisResult candidate,
                         GuideContextResult guideContext, GuideContextResultDto guideDto, String mainCategory,
                         String subCategory, String careerLevel) {
    }
}
