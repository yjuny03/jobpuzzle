package com.example.jobpuzzle.evaluation.service;

import com.example.jobpuzzle.ai.log.*;
import com.example.jobpuzzle.ai.dto.AnswerEvaluationResult;
import com.example.jobpuzzle.ai.dto.WeaknessAnswerEvaluationResult;
import com.example.jobpuzzle.ai.service.AiClientService;
import com.example.jobpuzzle.ai.service.GenerationClientSelection;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.ai.validation.AiResponseProcessor;
import com.example.jobpuzzle.evaluation.entity.*;
import com.example.jobpuzzle.evaluation.repository.AnswerEvaluationRepository;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagLogRepository;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagStatusRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.interview.entity.*;
import com.example.jobpuzzle.interview.repository.FollowUpQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AnswerEvaluationService {

    private static final int PASS_THRESHOLD = 70;
    private static final int MAX_FOLLOW_UP_DEPTH = 2;

    private final AnswerEvaluationRepository answerEvaluationRepository;
    private final InterviewMessageRepository interviewMessageRepository;
    private final FollowUpQuestionRepository followUpQuestionRepository;
    private final WeaknessTagLogRepository weaknessTagLogRepository;
    private final WeaknessTagStatusRepository weaknessTagStatusRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;
    private final AiResponseProcessor aiResponseProcessor;

    public EvaluationOutcome evaluateAnswer(InterviewMessage answerMessage) {
        InterviewSessionQuestion sessionQuestion = answerMessage.getSessionQuestion();
        InterviewSession session = sessionQuestion.getSession();
        InterviewSessionMode mode = session.getMode();

        String targetJson = mode == InterviewSessionMode.WEAKNESS_REVIEW ? "JSON-10" : "JSON-06";
        PromptTemplate template = promptTemplateRepository
                .findFirstByTargetJsonAndIsActiveTrueOrderByPromptTemplateIdDesc(targetJson)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_PROMPT_TEMPLATE_NOT_FOUND));

        AiExecutionStage stage = mode == InterviewSessionMode.WEAKNESS_REVIEW
                ? AiExecutionStage.WEAKNESS_REEVALUATION
                : AiExecutionStage.ANSWER_EVALUATION;
        GenerationClientSelection selection = aiClientService.resolve(stage);
        AiCallLog callLog = AiCallLog.pending(
                selection.provider(),
                selection.model(),
                stage,
                AiInputReferenceType.ANSWER,
                String.valueOf(answerMessage.getMessageId()),
                Integer.toHexString(answerMessage.getMessageText().hashCode()),
                template,
                session.getGuideDocument(),
                null
        );
        callLog.start();
        // IDENTITY PK는 save 즉시 INSERT될 수 있으므로 필수 startedAt을 먼저 채운다.
        aiCallLogRepository.save(callLog);

        EvaluationPayload payload;
        try {
            payload = selection.provider() == AiProvider.MOCK
                    ? mockPayload(answerMessage, sessionQuestion, mode)
                    : anthropicPayload(selection, template, answerMessage, sessionQuestion, mode);
        } catch (RuntimeException exception) {
            String failureTraceId = EvaluationFailureDiagnostics.newTraceId();
            AiCallLogErrorType errorType = EvaluationFailureDiagnostics.errorType(exception);
            callLog.fail(errorType, exception.getClass().getSimpleName());
            log.warn(
                    "ai_evaluation_failed traceId={} aiCallLogId={} sessionId={} sessionQuestionId={} "
                            + "answerMessageId={} mode={} stage={} provider={} model={} answerLength={} "
                            + "errorType={} rootCause={}",
                    failureTraceId,
                    callLog.getAiCallLogId(),
                    session.getSessionId(),
                    sessionQuestion.getSessionQuestionId(),
                    answerMessage.getMessageId(),
                    mode,
                    stage,
                    selection.provider(),
                    selection.model(),
                    answerMessage.getMessageText().length(),
                    errorType,
                    EvaluationFailureDiagnostics.rootCauseSummary(exception),
                    exception
            );
            // 답변 저장과 FAILED 로그는 유지한다. 실패 평가를 0점으로 만들지 않고
            // 집계에서 제외하면 사용자는 재접속·종료 흐름을 계속 사용할 수 있다.
            return new EvaluationOutcome(null, null, true, failureTraceId);
        }

        AnswerEvaluation evaluation = AnswerEvaluation.create(
                answerMessage,
                sessionQuestion,
                mode,
                payload.score(),
                payload.passThreshold(),
                mode == InterviewSessionMode.WEAKNESS_REVIEW ? null : "답변 기준 충족도",
                payload.details(),
                session.getTargetWeaknessTag(),
                session.getTargetDimension(),
                payload.weaknessTags(),
                payload.summary(),
                payload.improvementDirection(),
                callLog
        );
        answerEvaluationRepository.save(evaluation);
        registerWeaknessTags(evaluation);

        InterviewMessage followUpMessage = createFollowUpIfNeeded(
                evaluation, answerMessage, payload.followUpQuestion(), payload.followUpType(), payload.followUpReason()
        );
        callLog.succeed();
        return new EvaluationOutcome(evaluation, followUpMessage, false, null);
    }

    @Transactional(readOnly = true)
    public AnswerEvaluation getEvaluation(Long userId, Long evaluationId) {
        return answerEvaluationRepository
                .findByEvaluationIdAndSessionQuestion_Session_User_UserId(evaluationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
    }

    private InterviewMessage createFollowUpIfNeeded(
            AnswerEvaluation evaluation,
            InterviewMessage answerMessage,
            String aiQuestion,
            FollowUpQuestionType aiType,
            String aiReason
    ) {
        if (evaluation.passed()) {
            return null;
        }
        long currentDepth = interviewMessageRepository
                .countBySessionQuestion_SessionQuestionIdAndMessageType(
                        evaluation.getSessionQuestion().getSessionQuestionId(),
                        InterviewMessageType.FOLLOW_UP_QUESTION
                );
        if (currentDepth >= MAX_FOLLOW_UP_DEPTH) {
            return null;
        }

        String questionText = aiQuestion != null && !aiQuestion.isBlank() ? aiQuestion : currentDepth == 0
                ? "방금 답변에서 본인이 직접 수행한 행동을 더 구체적으로 설명해 주세요."
                : "그 행동으로 만들어진 결과를 수치나 객관적인 근거로 설명해 주세요.";
        InterviewMessage questionMessage = InterviewMessage.followUpQuestion(
                evaluation.getSessionQuestion(),
                answerMessage,
                questionText
        );
        interviewMessageRepository.save(questionMessage);
        followUpQuestionRepository.save(FollowUpQuestion.create(
                evaluation,
                questionMessage,
                aiType != null ? aiType : currentDepth == 0 ? FollowUpQuestionType.ROLE_CHECK : FollowUpQuestionType.RESULT_CHECK,
                evaluation.getWeaknessTags().isEmpty() ? null : evaluation.getWeaknessTags().get(0),
                aiReason != null ? aiReason : "기준 점수 미달 답변의 핵심 근거 추가 확인"
        ));
        return questionMessage;
    }

    private void registerWeaknessTags(AnswerEvaluation evaluation) {
        if (evaluation.getEvaluationMode() != InterviewSessionMode.COMPANY_FIT) {
            return;
        }
        for (String tag : evaluation.getWeaknessTags()) {
            if (weaknessTagLogRepository.existsByEvaluation_EvaluationIdAndTag(
                    evaluation.getEvaluationId(),
                    tag
            )) {
                continue;
            }
            WeaknessTagStatus status = weaknessTagStatusRepository
                    .findByUser_UserIdAndTag(
                            evaluation.getSessionQuestion().getSession().getUser().getUserId(),
                            tag
                    )
                    .orElse(null);
            WeaknessOccurrenceType occurrenceType;
            if (status == null) {
                status = WeaknessTagStatus.detected(
                        evaluation.getSessionQuestion().getSession().getUser(),
                        tag
                );
                weaknessTagStatusRepository.save(status);
                occurrenceType = WeaknessOccurrenceType.DETECTED;
            } else {
                occurrenceType = status.recur();
            }
            weaknessTagLogRepository.save(WeaknessTagLog.create(
                    evaluation.getSessionQuestion().getSession().getUser(),
                    evaluation.getSessionQuestion().getSession(),
                    evaluation,
                    tag,
                    occurrenceType
            ));
        }
    }

    private int mockScore(String answer) {
        return Math.min(90, 40 + Math.max(0, answer.trim().length() / 4));
    }

    private EvaluationPayload mockPayload(
            InterviewMessage answer,
            InterviewSessionQuestion question,
            InterviewSessionMode mode
    ) {
        int score = mockScore(answer.getMessageText());
        return new EvaluationPayload(
                score, PASS_THRESHOLD, buildDetails(question, mode, score),
                mode == InterviewSessionMode.COMPANY_FIT && score < PASS_THRESHOLD ? weaknessTags(question) : List.of(),
                score >= PASS_THRESHOLD ? "핵심 근거가 확인되었습니다." : "구체적인 근거를 보완해야 합니다.",
                score >= PASS_THRESHOLD ? List.of() : List.of("상황·본인 역할·행동·결과를 구체적으로 설명하세요."),
                null, null, null
        );
    }

    private EvaluationPayload anthropicPayload(
            GenerationClientSelection selection,
            PromptTemplate template,
            InterviewMessage answer,
            InterviewSessionQuestion question,
            InterviewSessionMode mode
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("interviewMode", mode.name());
        input.put("currentFollowUpDepth", interviewMessageRepository.countBySessionQuestion_SessionQuestionIdAndMessageType(
                question.getSessionQuestionId(), InterviewMessageType.FOLLOW_UP_QUESTION));
        input.put("question", question.getQuestionTextSnapshot());
        input.put("intent", question.getIntentSnapshot());
        input.put("evaluationFocus", question.getEvaluationFocusSnapshot());
        input.put("answer", answer.getMessageText());
        input.put("targetWeaknessTag", question.getSession().getTargetWeaknessTag());
        input.put("targetDimension", question.getSession().getTargetDimension());
        try {
            String prompt = template.getTemplateText() + "\n\n[INPUT]\n" + objectMapper.writeValueAsString(input);
            if (mode == InterviewSessionMode.WEAKNESS_REVIEW) {
                WeaknessAnswerEvaluationResult value =
                        aiResponseProcessor.parseWeaknessAnswerEvaluation(
                                callEvaluationWithRetry(selection, prompt, true));
                validateWeaknessResult(value, question, input.get("currentFollowUpDepth"));
                Map<String, AnswerEvaluation.DimensionEvaluation> details = Map.of(
                        value.getTargetDimension(),
                        new AnswerEvaluation.DimensionEvaluation(value.getScore(), value.getComment())
                );
                return new EvaluationPayload(
                        averageScore(details), PASS_THRESHOLD, details, List.of(), value.getComment(), List.of(),
                        value.getFollowUp() == null ? null : value.getFollowUp().getQuestion(),
                        value.getFollowUp() == null ? null : value.getFollowUp().getType(),
                        value.getFollowUp() == null ? null : value.getFollowUp().getReason()
                );
            }
            AnswerEvaluationResult value =
                    aiResponseProcessor.parseAnswerEvaluation(
                            callEvaluationWithRetry(selection, prompt, false));
            Map<String, AnswerEvaluation.DimensionEvaluation> details =
                    evaluationDetails(value, question.getEvaluationFocusSnapshot());
            validateGeneralResult(value, mode, input.get("currentFollowUpDepth"), details);
            return new EvaluationPayload(
                    averageScore(details), PASS_THRESHOLD, details,
                    value.getWeaknessTags() == null ? List.of() : value.getWeaknessTags(),
                    value.getSummary(),
                    value.getImprovementDirection() == null ? List.of() : value.getImprovementDirection(),
                    value.getFollowUp() == null ? null : value.getFollowUp().getQuestion(),
                    value.getFollowUp() == null ? null : value.getFollowUp().getType(),
                    value.getFollowUp() == null ? null : value.getFollowUp().getReason()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("AI answer evaluation failed", exception);
        }
    }

    private String callEvaluationWithRetry(
            GenerationClientSelection selection,
            String prompt,
            boolean weakness
    ) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return weakness
                        ? aiClientService.evaluateWeaknessAnswer(selection, prompt)
                        : aiClientService.evaluateAnswer(selection, prompt);
            } catch (RuntimeException exception) {
                last = exception;
            }
        }
        throw last == null ? new IllegalStateException("AI evaluation failed") : last;
    }

    private Map<String, AnswerEvaluation.DimensionEvaluation> evaluationDetails(
            AnswerEvaluationResult value,
            List<InterviewQuestionEvaluationFocus> focuses
    ) {
        Map<String, AnswerEvaluation.DimensionEvaluation> result = new LinkedHashMap<>();
        for (InterviewQuestionEvaluationFocus focus : focuses) {
            AnswerEvaluationResult.DimensionScore score = switch (focus) {
                case intentMatch -> value.getEvaluationDetail().getIntentMatch();
                case specificity -> value.getEvaluationDetail().getSpecificity();
                case ownRole -> value.getEvaluationDetail().getOwnRole();
                case problemSolving -> value.getEvaluationDetail().getProblemSolving();
                case resultExpression -> value.getEvaluationDetail().getResultExpression();
                case requirementConnection -> value.getEvaluationDetail().getRequirementConnection();
                case guideAlignment -> value.getEvaluationDetail().getGuideAlignment();
                case deliveryClarity -> value.getEvaluationDetail().getDeliveryClarity();
            };
            if (score != null && score.getScore() != null) {
                result.put(focus.name(), new AnswerEvaluation.DimensionEvaluation(score.getScore(), score.getComment()));
            }
        }
        return result;
    }

    private void validateGeneralResult(
            AnswerEvaluationResult value,
            InterviewSessionMode mode,
            Object expectedDepth,
            Map<String, AnswerEvaluation.DimensionEvaluation> details
    ) {
        if (value == null
                || value.getInterviewMode() != mode
                || value.getCurrentFollowUpDepth() != ((Number) expectedDepth).intValue()
                || value.getEvaluationDetail() == null
                || value.getPassThreshold() != PASS_THRESHOLD
                || details.isEmpty()) {
            throw new IllegalStateException("JSON-06 response does not match the evaluation contract");
        }
        validateScores(details);
    }

    private void validateWeaknessResult(
            WeaknessAnswerEvaluationResult value,
            InterviewSessionQuestion question,
            Object expectedDepth
    ) {
        if (value == null
                || !java.util.Objects.equals(
                        value.getTargetWeaknessTag(),
                        question.getSession().getTargetWeaknessTag())
                || !java.util.Objects.equals(
                        value.getTargetDimension(),
                        question.getSession().getTargetDimension())
                || value.getCurrentFollowUpDepth() != ((Number) expectedDepth).intValue()
                || value.getPassThreshold() != PASS_THRESHOLD
                || value.getScore() < 0
                || value.getScore() > 100
                || value.isPassed() != (value.getScore() >= PASS_THRESHOLD)) {
            throw new IllegalStateException("JSON-10 response does not match the evaluation contract");
        }
    }

    private void validateScores(
            Map<String, AnswerEvaluation.DimensionEvaluation> details
    ) {
        if (details.values().stream().anyMatch(detail ->
                detail.getScore() == null || detail.getScore() < 0 || detail.getScore() > 100)) {
            throw new IllegalStateException("evaluation dimension score must be between 0 and 100");
        }
    }

    private int averageScore(
            Map<String, AnswerEvaluation.DimensionEvaluation> details
    ) {
        validateScores(details);
        return (int) Math.round(details.values().stream()
                .map(AnswerEvaluation.DimensionEvaluation::getScore)
                .mapToInt(Integer::intValue)
                .average()
                .orElseThrow(() -> new IllegalStateException("evaluation detail is empty")));
    }

    private record EvaluationPayload(
            int score,
            int passThreshold,
            Map<String, AnswerEvaluation.DimensionEvaluation> details,
            List<String> weaknessTags,
            String summary,
            List<String> improvementDirection,
            String followUpQuestion,
            FollowUpQuestionType followUpType,
            String followUpReason
    ) {}

    private Map<String, AnswerEvaluation.DimensionEvaluation> buildDetails(
            InterviewSessionQuestion question,
            InterviewSessionMode mode,
            int score
    ) {
        Map<String, AnswerEvaluation.DimensionEvaluation> details = new LinkedHashMap<>();
        if (mode == InterviewSessionMode.WEAKNESS_REVIEW) {
            details.put(
                    question.getSession().getTargetDimension(),
                    new AnswerEvaluation.DimensionEvaluation(score, "선택 약점 관점 재평가")
            );
            return details;
        }
        // 질문 생성 시 확정한 evaluationFocus만 평가한다. 질문이 요구하지 않은 관점을
        // 낮은 점수로 만드는 대신 집계 대상 자체에서 제외한다.
        for (InterviewQuestionEvaluationFocus focus : question.getEvaluationFocusSnapshot()) {
            if (mode == InterviewSessionMode.BASIC
                    && focus == InterviewQuestionEvaluationFocus.requirementConnection) {
                continue;
            }
            details.put(
                    focus.name(),
                    new AnswerEvaluation.DimensionEvaluation(score, "Mock 기준 평가")
            );
        }
        return details;
    }

    private List<String> weaknessTags(InterviewSessionQuestion question) {
        return question.getEvaluationFocusSnapshot().stream()
                .filter(focus -> focus != InterviewQuestionEvaluationFocus.requirementConnection)
                .map(focus -> focus.name() + "Weak")
                .distinct()
                .toList();
    }

    public record EvaluationOutcome(
            AnswerEvaluation evaluation,
            InterviewMessage followUpMessage,
            boolean evaluationFailed,
            String failureTraceId
    ) {
        public EvaluationOutcome(
                AnswerEvaluation evaluation,
                InterviewMessage followUpMessage,
                boolean evaluationFailed
        ) {
            this(evaluation, followUpMessage, evaluationFailed, null);
        }
    }
}
