package com.example.jobpuzzle.evaluation.service;

import com.example.jobpuzzle.ai.log.*;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
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
        AiCallLog log = AiCallLog.pending(
                AiProvider.MOCK,
                "mock-interview-v1",
                stage,
                AiInputReferenceType.ANSWER,
                String.valueOf(answerMessage.getMessageId()),
                Integer.toHexString(answerMessage.getMessageText().hashCode()),
                template,
                session.getGuideDocument(),
                null
        );
        aiCallLogRepository.save(log);
        log.start();

        int score = mockScore(answerMessage.getMessageText());
        Map<String, AnswerEvaluation.DimensionEvaluation> details =
                buildDetails(sessionQuestion, mode, score);
        List<String> weaknessTags = mode == InterviewSessionMode.COMPANY_FIT && score < PASS_THRESHOLD
                ? weaknessTags(sessionQuestion)
                : List.of();

        AnswerEvaluation evaluation = AnswerEvaluation.create(
                answerMessage,
                sessionQuestion,
                mode,
                score,
                PASS_THRESHOLD,
                mode == InterviewSessionMode.WEAKNESS_REVIEW ? null : "답변 기준 충족도",
                details,
                session.getTargetWeaknessTag(),
                session.getTargetDimension(),
                weaknessTags,
                score >= PASS_THRESHOLD ? "핵심 근거가 확인되었습니다." : "구체적인 근거를 보완해야 합니다.",
                score >= PASS_THRESHOLD ? List.of() : List.of("상황·본인 역할·행동·결과를 구체적으로 설명하세요."),
                log
        );
        answerEvaluationRepository.save(evaluation);
        registerWeaknessTags(evaluation);

        InterviewMessage followUpMessage = createFollowUpIfNeeded(evaluation, answerMessage);
        log.succeed();
        return new EvaluationOutcome(evaluation, followUpMessage);
    }

    @Transactional(readOnly = true)
    public AnswerEvaluation getEvaluation(Long userId, Long evaluationId) {
        return answerEvaluationRepository
                .findByEvaluationIdAndSessionQuestion_Session_User_UserId(evaluationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
    }

    private InterviewMessage createFollowUpIfNeeded(
            AnswerEvaluation evaluation,
            InterviewMessage answerMessage
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

        String questionText = currentDepth == 0
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
                currentDepth == 0 ? FollowUpQuestionType.ROLE_CHECK : FollowUpQuestionType.RESULT_CHECK,
                evaluation.getWeaknessTags().isEmpty() ? null : evaluation.getWeaknessTags().get(0),
                "기준 점수 미달 답변의 핵심 근거 추가 확인"
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
        for (InterviewQuestionEvaluationFocus focus : InterviewQuestionEvaluationFocus.values()) {
            Integer dimensionScore = focus == InterviewQuestionEvaluationFocus.requirementConnection
                    && mode == InterviewSessionMode.BASIC
                    ? null
                    : score;
            details.put(
                    focus.name(),
                    new AnswerEvaluation.DimensionEvaluation(dimensionScore, "Mock 기준 평가")
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
            InterviewMessage followUpMessage
    ) {
    }
}
