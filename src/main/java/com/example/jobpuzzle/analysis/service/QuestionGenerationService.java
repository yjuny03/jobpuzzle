package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.log.*;
import com.example.jobpuzzle.ai.service.GenerationClientSelection;
import com.example.jobpuzzle.ai.dto.InterviewQuestionGenerationResult;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRenderer;
import com.example.jobpuzzle.ai.service.AiClientService;
import com.example.jobpuzzle.ai.validation.AiResponseProcessor;
import com.example.jobpuzzle.ai.validation.InterviewQuestionGenerationResponseValidator;
import com.example.jobpuzzle.analysis.dto.BasicQuestionRequest;
import com.example.jobpuzzle.analysis.dto.QuestionSetResponse;
import com.example.jobpuzzle.analysis.dto.PreparedQuestionSetResponse;
import com.example.jobpuzzle.analysis.dto.WeaknessQuestionRequest;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import com.example.jobpuzzle.evaluation.entity.WeaknessRemediationAttempt;
import com.example.jobpuzzle.evaluation.entity.WeaknessRemediationStatus;
import com.example.jobpuzzle.evaluation.entity.WeaknessTagLog;
import com.example.jobpuzzle.evaluation.entity.WeaknessTagResolveStatus;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagLogRepository;
import com.example.jobpuzzle.evaluation.repository.WeaknessRemediationAttemptRepository;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagStatusRepository;
import com.example.jobpuzzle.evaluation.service.WeaknessTagNormalizer;
import com.example.jobpuzzle.evaluation.service.SessionScoreAggregationService;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.interview.entity.*;
import com.example.jobpuzzle.interview.dto.QuestionHintResponse;
import com.example.jobpuzzle.interview.dto.QuestionListResponse;
import com.example.jobpuzzle.interview.repository.InterviewQuestionRepository;
import com.example.jobpuzzle.interview.repository.QuestionSetRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionQuestionRepository;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuestionGenerationService {

    private final UserRepository userRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final QuestionSetRepository questionSetRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewSessionQuestionRepository interviewSessionQuestionRepository;
    private final WeaknessTagStatusRepository weaknessTagStatusRepository;
    private final WeaknessTagLogRepository weaknessTagLogRepository;
    private final WeaknessRemediationAttemptRepository weaknessRemediationAttemptRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final AiClientService aiClientService;
    private final PromptTemplateRenderer promptTemplateRenderer;
    private final AiResponseProcessor aiResponseProcessor;
    private final InterviewQuestionGenerationInputMapper inputMapper;
    private final InterviewQuestionGenerationResponseValidator responseValidator;
    private final InterviewQuestionGenerationResultWriter resultWriter;
    private final WeaknessTagNormalizer weaknessTagNormalizer;
    private final SessionScoreAggregationService sessionScoreAggregationService;

    public QuestionSetResponse generateBasicQuestionSet(Long userId, BasicQuestionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        JobCategory jobCategory = jobCategoryRepository.findById(request.getJobCategoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND));
        if (jobCategory.getCareerLevel() != request.getCareerLevel()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        PromptTemplate template = activeTemplate("JSON-11");
        String prompt = promptTemplateRenderer.renderInterviewQuestionGeneration(
                template,
                "basicInputJson",
                inputMapper.basic(jobCategory)
        );
        AiCallLog callLog = startLog(
                template,
                AiExecutionStage.BASIC_QUESTION_GENERATION,
                AiInputReferenceType.JOB_CAREER_CRITERIA,
                jobCategory.getJobCategoryId() + ":" + request.getCareerLevel(),
                prompt
        );
        String rawResponse = aiClientService.generateBasicQuestions(prompt);
        InterviewQuestionGenerationResult generated =
                aiResponseProcessor.parseInterviewQuestions(rawResponse, "JSON-11");
        responseValidator.validateBasic(generated);

        QuestionSet set = questionSetRepository.save(QuestionSet.create(
                user,
                InterviewSessionMode.BASIC,
                jobCategory,
                request.getCareerLevel(),
                null,
                QuestionSetGenerationSource.AI,
                callLog.getPromptVersion(),
                null,
                null,
                List.of(),
                callLog
        ));

        List<InterviewQuestion> questions = resultWriter.saveBasic(set, generated);
        callLog.succeed();
        return QuestionSetResponse.from(set, questions);
    }

    public QuestionSetResponse generateWeaknessQuestionSet(
            Long userId,
            WeaknessQuestionRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String canonicalTag = weaknessTagNormalizer.canonicalTag(request.getTargetWeaknessTag());
        boolean unresolved = weaknessTagStatusRepository
                .findByUser_UserIdAndStatus(userId, WeaknessTagResolveStatus.UNRESOLVED)
                .stream()
                .anyMatch(status -> weaknessTagNormalizer.sameDimension(status.getTag(), canonicalTag));
        if (!unresolved) {
            throw new CustomException(ErrorCode.WEAKNESS_NOT_AVAILABLE);
        }

        List<WeaknessTagLog> origins = weaknessTagLogRepository
                .findByUser_UserIdOrderByTagLogIdDesc(userId)
                .stream()
                .filter(logEntry -> weaknessTagNormalizer.sameDimension(logEntry.getTag(), canonicalTag))
                .filter(logEntry -> logEntry.getEvaluation() != null)
                .toList();
        Map<Long, List<WeaknessTagLog>> originsBySession = origins.stream()
                .filter(origin -> originSessionId(origin) != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        this::originSessionId,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        Map<Long, List<WeaknessRemediationAttempt>> attemptsByOriginLog =
                weaknessRemediationAttemptRepository
                        .findByOriginTagLog_User_UserIdOrderByAttemptIdDesc(userId)
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                attempt -> attempt.getOriginTagLog().getTagLogId(),
                                LinkedHashMap::new,
                                java.util.stream.Collectors.toList()
                        ));
        List<AnswerEvaluation> generationSources = originsBySession.values().stream()
                .map(sessionLogs -> generationSourceForSession(sessionLogs, attemptsByOriginLog))
                .filter(java.util.Objects::nonNull)
                .limit(10)
                .toList();
        if (generationSources.isEmpty()) {
            throw new CustomException(ErrorCode.WEAKNESS_NOT_AVAILABLE);
        }

        // 약점 로그가 이미 원본 세션을 보존하므로, 구형 평가 데이터의 연관관계가
        // 일부 비어 있어도 약점 질문 세트를 다시 만들 수 있다.
        InterviewSession originSession =
                generationSources.get(0).getSessionQuestion().getSession();
        if (originSession == null) {
            throw new CustomException(ErrorCode.WEAKNESS_NOT_AVAILABLE);
        }
        String targetDimension = weaknessTagNormalizer.dimension(canonicalTag);
        Map<Long, InterviewQuestion> reusableByEvaluationId = new LinkedHashMap<>();
        generationSources.forEach(source -> interviewSessionQuestionRepository
                .findFirstBySession_User_UserIdAndStatusAndQuestion_OriginEvaluation_EvaluationIdOrderBySessionQuestionIdDesc(
                        userId,
                        InterviewSessionQuestionStatus.SKIPPED,
                        source.getEvaluationId())
                .map(InterviewSessionQuestion::getQuestion)
                .ifPresent(question -> reusableByEvaluationId.put(
                        source.getEvaluationId(), question)));
        List<AnswerEvaluation> sourcesToGenerate = generationSources.stream()
                .filter(source -> !reusableByEvaluationId.containsKey(source.getEvaluationId()))
                .toList();
        List<Long> basisEvaluationIds = generationSources.stream()
                .map(AnswerEvaluation::getEvaluationId)
                .distinct()
                .toList();

        PromptTemplate template = activeTemplate("JSON-09");
        String prompt = promptTemplateRenderer.renderInterviewQuestionGeneration(
                template,
                "weaknessInputJson",
                inputMapper.weaknessEvaluations(canonicalTag, targetDimension, sourcesToGenerate)
        );
        AiCallLog callLog = startLog(
                template,
                AiExecutionStage.WEAKNESS_QUESTION_GENERATION,
                AiInputReferenceType.WEAKNESS_TAG,
                userId + ":" + canonicalTag,
                prompt
        );
        InterviewQuestionGenerationResult generated;
        RuntimeException lastFailure = null;
        try {
            generated = sourcesToGenerate.isEmpty()
                    ? new InterviewQuestionGenerationResult(List.of())
                    : null;
            for (int attempt = 1; generated == null && attempt <= 3; attempt++) {
                String rawResponse = null;
                try {
                    rawResponse = aiClientService.generateWeaknessQuestions(prompt);
                    generated =
                            aiResponseProcessor.parseInterviewQuestions(rawResponse, "JSON-09");
                    generated = normalizeWeaknessQuestionMetadata(
                            generated,
                            sourcesToGenerate,
                            canonicalTag,
                            targetDimension
                    );
                    responseValidator.validateWeakness(
                            generated,
                            canonicalTag,
                            targetDimension,
                            sourcesToGenerate.stream()
                                    .map(AnswerEvaluation::getEvaluationId)
                                    .toList()
                    );
                    break;
                } catch (RuntimeException exception) {
                    RuntimeException loggedFailure = rawResponse == null
                            ? exception
                            : new IllegalStateException(
                                    "providerResponsePreview=" + responsePreview(rawResponse),
                                    exception
                            );
                    lastFailure = loggedFailure;
                    log.warn(
                            "ai_weakness_question_generation_retry attempt={} maxAttempts=3 "
                                    + "userId={} weaknessTag={} errorType={} failureDetail={}",
                            attempt,
                            userId,
                            canonicalTag,
                            generationErrorType(loggedFailure),
                            failureDetail(loggedFailure)
                    );
                }
            }
            if (generated == null) {
                throw lastFailure == null
                        ? new IllegalStateException("AI weakness question generation failed")
                        : lastFailure;
            }
        } catch (RuntimeException exception) {
            String traceId = "WEAK-Q-" + java.util.UUID.randomUUID()
                    .toString().replace("-", "").substring(0, 12).toUpperCase();
            AiCallLogErrorType errorType = generationErrorType(exception);
            callLog.fail(errorType, traceId + " | " + errorType);
            log.error(
                    "ai_weakness_question_generation_failed traceId={} aiCallLogId={} "
                            + "userId={} weaknessTag={} promptVersion={} errorType={} failureDetail={}",
                    traceId,
                    callLog.getAiCallLogId(),
                    userId,
                    canonicalTag,
                    callLog.getPromptVersion(),
                    errorType,
                    failureDetail(exception),
                    exception
            );
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }

        QuestionSet set = questionSetRepository.save(QuestionSet.create(
                user,
                InterviewSessionMode.WEAKNESS_REVIEW,
                originSession.getJobCategory(),
                originSession.getCareerLevel(),
                originSession.getGuideContextResult(),
                QuestionSetGenerationSource.AI,
                callLog.getPromptVersion(),
                canonicalTag,
                targetDimension,
                basisEvaluationIds,
                callLog
        ));

        Map<Long, InterviewQuestionGenerationResult.Question> generatedByOrigin =
                generated.getQuestions().stream().collect(java.util.stream.Collectors.toMap(
                        InterviewQuestionGenerationResult.Question::getOriginEvaluationId,
                        value -> value
                ));
        List<InterviewQuestionGenerationResult.Question> combinedQuestions =
                generationSources.stream()
                        .map(source -> {
                            InterviewQuestion reusable =
                                    reusableByEvaluationId.get(source.getEvaluationId());
                            if (reusable == null) {
                                return generatedByOrigin.get(source.getEvaluationId());
                            }
                            return new InterviewQuestionGenerationResult.Question(
                                    "reused-" + source.getEvaluationId(),
                                    reusable.getQuestionType(),
                                    reusable.getQuestion(),
                                    reusable.getIntent(),
                                    reusable.getEvaluationFocus(),
                                    source.getEvaluationId(),
                                    canonicalTag,
                                    targetDimension,
                                    InterviewQuestionReviewStatus.PASS,
                                    null
                            );
                        })
                        .toList();
        InterviewQuestionGenerationResult combined =
                new InterviewQuestionGenerationResult(combinedQuestions);
        List<AnswerEvaluation> originEvaluations = generationSources;
        List<InterviewQuestion> questions =
                resultWriter.saveWeakness(set, combined, originEvaluations);
        callLog.succeed();
        Map<Long, Integer> aggregateOriginScores = new LinkedHashMap<>();
        generationSources.forEach(source -> {
            Integer aggregateScore =
                    aggregateDimensionScore(userId, source, targetDimension);
            if (aggregateScore != null) {
                aggregateOriginScores.put(source.getEvaluationId(), aggregateScore);
            }
        });
        return QuestionSetResponse.from(
                set,
                questions,
                aggregateOriginScores,
                weaknessTagNormalizer.displayName(canonicalTag)
        );
    }

    private Integer aggregateDimensionScore(
            Long userId,
            AnswerEvaluation source,
            String targetDimension
    ) {
        Integer fallback = source.getScore();
        try {
            InterviewSession session = source.getSessionQuestion().getSession();
            if (session == null) {
                return fallback;
            }
            return sessionScoreAggregationService
                    .aggregate(userId, session.getSessionId())
                    .getCategoryScores()
                    .getOrDefault(targetDimension, fallback);
        } catch (RuntimeException exception) {
            log.warn(
                    "weakness_question_origin_score_fallback userId={} evaluationId={} "
                            + "dimension={} fallback={} errorType={}",
                    userId,
                    source.getEvaluationId(),
                    targetDimension,
                    fallback,
                    exception.getClass().getSimpleName()
            );
            return fallback;
        }
    }

    private Long originSessionId(WeaknessTagLog origin) {
        if (origin.getSession() != null) {
            return origin.getSession().getSessionId();
        }
        if (origin.getEvaluation() != null
                && origin.getEvaluation().getSessionQuestion() != null
                && origin.getEvaluation().getSessionQuestion().getSession() != null) {
            return origin.getEvaluation().getSessionQuestion().getSession().getSessionId();
        }
        return null;
    }

    private AnswerEvaluation generationSourceForSession(
            List<WeaknessTagLog> sessionLogs,
            Map<Long, List<WeaknessRemediationAttempt>> attemptsByOriginLog
    ) {
        WeaknessRemediationAttempt latestAttempt = sessionLogs.stream()
                .flatMap(origin -> attemptsByOriginLog
                        .getOrDefault(origin.getTagLogId(), List.of()).stream())
                .max(Comparator.comparing(WeaknessRemediationAttempt::getAttemptId))
                .orElse(null);
        if (latestAttempt != null) {
            return latestAttempt.getStatus() == WeaknessRemediationStatus.RESOLVED
                    ? null
                    : latestAttempt.getEvaluation();
        }
        return sessionLogs.stream()
                .map(WeaknessTagLog::getEvaluation)
                .filter(java.util.Objects::nonNull)
                .filter(evaluation -> evaluation.getSessionQuestion() != null)
                .max(Comparator.comparing(AnswerEvaluation::getEvaluationId))
                .orElse(null);
    }

    private InterviewQuestionGenerationResult normalizeWeaknessQuestionMetadata(
            InterviewQuestionGenerationResult generated,
            List<AnswerEvaluation> expectedSources,
            String canonicalTag,
            String targetDimension
    ) {
        if (generated == null || generated.getQuestions() == null
                || generated.getQuestions().size() < expectedSources.size()) {
            return generated;
        }
        List<InterviewQuestionGenerationResult.Question> normalized =
                java.util.stream.IntStream.range(0, expectedSources.size())
                        .mapToObj(index -> {
                            InterviewQuestionGenerationResult.Question value =
                                    generated.getQuestions().get(index);
                            List<InterviewQuestionEvaluationFocus> focus =
                                    value.getEvaluationFocus() == null
                                            || value.getEvaluationFocus().isEmpty()
                                            ? List.of(InterviewQuestionEvaluationFocus
                                                    .valueOf(targetDimension))
                                            : value.getEvaluationFocus();
                            return new InterviewQuestionGenerationResult.Question(
                                    value.getQuestionId() == null || value.getQuestionId().isBlank()
                                            ? "weakness-" + expectedSources.get(index).getEvaluationId()
                                            : value.getQuestionId(),
                                    InterviewQuestionType.WEAKNESS_FOLLOWUP,
                                    value.getQuestion(),
                                    value.getIntent() == null || value.getIntent().isBlank()
                                            ? "선택한 약점 관점의 개선 여부를 확인합니다."
                                            : value.getIntent(),
                                    focus,
                                    expectedSources.get(index).getEvaluationId(),
                                    canonicalTag,
                                    targetDimension,
                                    InterviewQuestionReviewStatus.PASS,
                                    null
                            );
                        })
                        .toList();
        return new InterviewQuestionGenerationResult(normalized);
    }

    @Transactional(readOnly = true)
    public QuestionSetResponse getQuestionSet(Long userId, Long questionSetId) {
        QuestionSet set = questionSetRepository
                .findByQuestionSetIdAndUser_UserId(questionSetId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_SET_NOT_READY));
        List<InterviewQuestion> questions =
                interviewQuestionRepository.findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(questionSetId);
        return QuestionSetResponse.from(
                set,
                questions,
                Map.of(),
                set.getTargetWeaknessTag() == null
                        ? null
                        : weaknessTagNormalizer.displayName(set.getTargetWeaknessTag())
        );
    }

    @Transactional(readOnly = true)
    public List<PreparedQuestionSetResponse> getUnusedGeneratedQuestionSets(Long userId) {
        List<WeaknessTagLog> loadedWeaknessLogs =
                weaknessTagLogRepository.findByUser_UserIdOrderByTagLogIdDesc(userId);
        List<WeaknessTagLog> weaknessLogs =
                loadedWeaknessLogs == null ? List.of() : loadedWeaknessLogs;
        return List.of(InterviewSessionMode.BASIC, InterviewSessionMode.WEAKNESS_REVIEW).stream()
                .flatMap(mode -> questionSetRepository
                        .findByUser_UserIdAndInterviewModeAndStatus(
                                userId, mode, QuestionSetStatus.ACTIVE
                        )
                        .stream())
                .filter(set -> !interviewSessionRepository
                        .existsByQuestionSet_QuestionSetId(set.getQuestionSetId()))
                .map(set -> PreparedQuestionSetResponse.from(
                        set,
                        interviewQuestionRepository
                                .findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(
                                        set.getQuestionSetId()
                                )
                                .size(),
                        weaknessTagNormalizer,
                        set.getInterviewMode() == InterviewSessionMode.WEAKNESS_REVIEW
                                ? weaknessLogs.stream()
                                .filter(log -> weaknessTagNormalizer.sameDimension(
                                        log.getTag(), set.getTargetWeaknessTag()
                                ))
                                .toList()
                                : List.of()
                ))
                .sorted(java.util.Comparator.comparing(
                        PreparedQuestionSetResponse::getQuestionSetId
                ).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionListResponse> getQuestionList(Long userId, Long questionSetId) {
        questionSetRepository.findByQuestionSetIdAndUser_UserId(questionSetId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_SET_NOT_READY));
        return interviewQuestionRepository
                .findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(questionSetId)
                .stream()
                .filter(question -> question.getReviewStatus() == InterviewQuestionReviewStatus.PASS)
                .map(QuestionListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionHintResponse getQuestionHint(Long userId, Long questionId) {
        InterviewQuestion question = interviewQuestionRepository.findById(questionId)
                .filter(value -> value.getQuestionSet().getUser().getUserId().equals(userId))
                .filter(value -> value.getReviewStatus() == InterviewQuestionReviewStatus.PASS)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        return QuestionHintResponse.from(question);
    }

    private AiCallLog startLog(
            PromptTemplate template,
            AiExecutionStage stage,
            AiInputReferenceType referenceType,
            String referenceId,
            String renderedPrompt
    ) {
        GenerationClientSelection selection = aiClientService.resolve(stage);
        AiCallLog log = AiCallLog.pending(
                selection.provider(),
                selection.model(),
                stage,
                referenceType,
                referenceId,
                Integer.toHexString(renderedPrompt.hashCode()),
                template,
                null,
                null
        );
        log.start();
        // IDENTITY PK는 save 즉시 INSERT될 수 있으므로 필수 startedAt을 먼저 채운다.
        aiCallLogRepository.save(log);
        return log;
    }

    private PromptTemplate activeTemplate(String targetJson) {
        return promptTemplateRepository
                .findFirstByTargetJsonAndIsActiveTrueOrderByPromptTemplateIdDesc(targetJson)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_PROMPT_TEMPLATE_NOT_FOUND));
    }

    private AiCallLogErrorType generationErrorType(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof com.example.jobpuzzle.ai.validation.AiProcessingException processing) {
                return processing.getErrorType();
            }
            current = current.getCause();
        }
        return AiCallLogErrorType.RESPONSE_VALIDATION_FAILED;
    }

    private String failureDetail(Throwable failure) {
        StringBuilder detail = new StringBuilder();
        Throwable current = failure;
        while (current != null && detail.length() < 900) {
            if (!detail.isEmpty()) detail.append(" <- ");
            detail.append(current.getClass().getSimpleName()).append(": ")
                    .append(current.getMessage() == null ? "(no message)" : current.getMessage());
            current = current.getCause();
        }
        return responsePreview(detail.toString(), 900);
    }

    private String responsePreview(String value) {
        return responsePreview(value, 500);
    }

    private String responsePreview(String value, int limit) {
        if (value == null) return "(null)";
        String singleLine = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return singleLine.length() <= limit ? singleLine : singleLine.substring(0, limit);
    }
}
