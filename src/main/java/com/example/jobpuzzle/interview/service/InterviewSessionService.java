package com.example.jobpuzzle.interview.service;

import com.example.jobpuzzle.analysis.dto.CompanyFitQuestionSetResponse;
import com.example.jobpuzzle.analysis.service.CompanyFitQuestionSetQueryService;
import com.example.jobpuzzle.evaluation.service.AnswerEvaluationService;
import com.example.jobpuzzle.evaluation.entity.WeaknessTagResolveStatus;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagStatusRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.interview.dto.*;
import com.example.jobpuzzle.interview.entity.*;
import com.example.jobpuzzle.interview.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewSessionService {

    private static final List<InterviewSessionStatus> UNFINISHED_STATUSES =
            List.of(InterviewSessionStatus.CREATED, InterviewSessionStatus.IN_PROGRESS);

    private final QuestionSetRepository questionSetRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewSessionQuestionRepository sessionQuestionRepository;
    private final InterviewMessageRepository interviewMessageRepository;
    private final CompanyFitQuestionSetQueryService companyFitQuestionSetQueryService;
    private final AnswerEvaluationService answerEvaluationService;
    private final WeaknessTagStatusRepository weaknessTagStatusRepository;

    @Transactional(readOnly = true)
    public InterviewModeAvailabilityResponse getAvailableModes(Long userId) {
        boolean companyFitAvailable = questionSetRepository
                .existsByUser_UserIdAndInterviewModeAndStatus(
                        userId,
                        InterviewSessionMode.COMPANY_FIT,
                        QuestionSetStatus.ACTIVE
                );
        boolean weaknessAvailable = weaknessTagStatusRepository
                .existsByUser_UserIdAndStatus(userId, WeaknessTagResolveStatus.UNRESOLVED);
        return InterviewModeAvailabilityResponse.builder()
                .modes(List.of(
                        InterviewModeAvailabilityResponse.ModeAvailability.builder()
                                .mode(InterviewSessionMode.BASIC)
                                .available(true)
                                .reason(null)
                                .build(),
                        InterviewModeAvailabilityResponse.ModeAvailability.builder()
                                .mode(InterviewSessionMode.COMPANY_FIT)
                                .available(companyFitAvailable)
                                .reason(companyFitAvailable ? null : "완료된 회사 맞춤 질문 세트가 필요합니다.")
                                .build(),
                        InterviewModeAvailabilityResponse.ModeAvailability.builder()
                                .mode(InterviewSessionMode.WEAKNESS_REVIEW)
                                .available(weaknessAvailable)
                                .reason(weaknessAvailable ? null : "미해결 약점 태그가 필요합니다.")
                                .build()
                ))
                .build();
    }

    public SessionResponse createSession(Long userId, SessionCreateRequest request) {
        QuestionSet questionSet = questionSetRepository
                .findByQuestionSetIdAndUser_UserId(request.getQuestionSetId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_SET_NOT_READY));

        if (!questionSet.isActive()) {
            throw new CustomException(ErrorCode.QUESTION_SET_NOT_READY);
        }
        if (interviewSessionRepository.existsByQuestionSet_QuestionSetIdAndStatusIn(
                questionSet.getQuestionSetId(),
                UNFINISHED_STATUSES
        )) {
            throw new CustomException(ErrorCode.INTERVIEW_SESSION_ALREADY_EXISTS);
        }

        List<Long> selectedIds = request.getSelectedQuestionIds().stream().distinct().toList();
        if (selectedIds.isEmpty() || selectedIds.size() != request.getSelectedQuestionIds().size()) {
            throw new CustomException(ErrorCode.INVALID_QUESTION_SELECTION);
        }

        if (questionSet.getInterviewMode() == InterviewSessionMode.COMPANY_FIT) {
            validateCompanyFitSelection(userId, questionSet, selectedIds);
        }

        List<InterviewQuestion> selectedQuestions =
                interviewQuestionRepository.findByQuestionSet_QuestionSetIdAndQuestionIdIn(
                        questionSet.getQuestionSetId(),
                        selectedIds
                );
        if (selectedQuestions.size() != selectedIds.size()
                || selectedQuestions.stream().anyMatch(q -> q.getReviewStatus() != InterviewQuestionReviewStatus.PASS)) {
            throw new CustomException(ErrorCode.INVALID_QUESTION_SELECTION);
        }
        selectedQuestions.sort(Comparator.comparingInt(InterviewQuestion::getDisplayOrder));

        InterviewSession session = interviewSessionRepository.save(InterviewSession.create(questionSet));
        int order = 1;
        for (InterviewQuestion question : selectedQuestions) {
            InterviewSessionQuestion snapshot = sessionQuestionRepository.save(
                    InterviewSessionQuestion.snapshot(session, question, order++)
            );
            interviewMessageRepository.save(InterviewMessage.originalQuestion(snapshot));
        }
        return SessionResponse.from(session, selectedQuestions.size());
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(Long userId, Long sessionId) {
        InterviewSession session = getOwnedSession(userId, sessionId);
        return SessionResponse.from(
                session,
                Math.toIntExact(sessionQuestionRepository.countBySession_SessionId(sessionId))
        );
    }

    @Transactional(readOnly = true)
    public List<SessionQuestionResponse> getSessionQuestions(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);
        return sessionQuestionRepository.findBySession_SessionIdOrderByDisplayOrderAsc(sessionId)
                .stream()
                .map(SessionQuestionResponse::from)
                .toList();
    }

    public SessionResponse startSession(Long userId, Long sessionId) {
        InterviewSession session = getOwnedSession(userId, sessionId);
        validateEditable(session);
        session.start();
        return SessionResponse.from(
                session,
                Math.toIntExact(sessionQuestionRepository.countBySession_SessionId(sessionId))
        );
    }

    @Transactional(readOnly = true)
    public SessionQuestionResponse getNextQuestion(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);
        return sessionQuestionRepository
                .findFirstBySession_SessionIdAndStatusInOrderByDisplayOrderAsc(
                        sessionId,
                        List.of(
                                InterviewSessionQuestionStatus.PENDING,
                                InterviewSessionQuestionStatus.IN_PROGRESS
                        )
                )
                .map(SessionQuestionResponse::from)
                .orElse(null);
    }

    public AnswerSubmitResponse submitAnswer(
            Long userId,
            Long sessionQuestionId,
            AnswerSubmitRequest request
    ) {
        InterviewSessionQuestion sessionQuestion = sessionQuestionRepository
                .findBySessionQuestionIdAndSession_User_UserId(sessionQuestionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_QUESTION_NOT_FOUND));
        InterviewSession session = sessionQuestion.getSession();
        validateEditable(session);

        InterviewMessage parentQuestion = resolveParentQuestion(userId, sessionQuestion, request);
        if (interviewMessageRepository.existsByParentMessage_MessageIdAndSender(
                parentQuestion.getMessageId(),
                InterviewMessageSender.USER
        )) {
            throw new CustomException(ErrorCode.ANSWER_ALREADY_CONFIRMED);
        }

        session.start();
        sessionQuestion.start();
        InterviewMessage answer = interviewMessageRepository.save(InterviewMessage.answer(
                sessionQuestion,
                parentQuestion,
                request.getAnswerType(),
                request.getMessageText().trim()
        ));

        AnswerEvaluationService.EvaluationOutcome outcome =
                answerEvaluationService.evaluateAnswer(answer);
        if (outcome.followUpMessage() == null) {
            sessionQuestion.complete();
        }

        SessionQuestionResponse nextQuestion = outcome.followUpMessage() == null
                ? getNextQuestion(userId, session.getSessionId())
                : SessionQuestionResponse.from(sessionQuestion);
        return AnswerSubmitResponse.builder()
                .answerMessageId(answer.getMessageId())
                .evaluationId(outcome.evaluation().getEvaluationId())
                .score(outcome.evaluation().getScore())
                .summary(outcome.evaluation().getSummary())
                .followUpQuestionMessageId(
                        outcome.followUpMessage() == null ? null : outcome.followUpMessage().getMessageId()
                )
                .followUpQuestion(
                        outcome.followUpMessage() == null ? null : outcome.followUpMessage().getMessageText()
                )
                .sessionStatus(session.getStatus())
                .nextQuestion(nextQuestion)
                .build();
    }

    public SessionResponse completeSession(Long userId, Long sessionId) {
        InterviewSession session = getOwnedSession(userId, sessionId);
        validateEditable(session);

        boolean hasAnswer = interviewMessageRepository
                .existsBySessionQuestion_Session_SessionIdAndMessageTypeIn(
                        sessionId,
                        List.of(
                                InterviewMessageType.ORIGINAL_ANSWER,
                                InterviewMessageType.FOLLOW_UP_ANSWER
                        )
                );
        if (!hasAnswer || hasUnansweredFollowUp(sessionId)) {
            throw new CustomException(ErrorCode.SESSION_CANNOT_BE_COMPLETED);
        }

        sessionQuestionRepository.findBySession_SessionIdOrderByDisplayOrderAsc(sessionId)
                .forEach(InterviewSessionQuestion::skip);
        session.complete();

        // 면접 기능 추가: 정상 완료된 QuestionSet은 미선택 질문까지 더 이상 재사용하지 않는다.
        session.getQuestionSet().archive();
        return SessionResponse.from(
                session,
                Math.toIntExact(sessionQuestionRepository.countBySession_SessionId(sessionId))
        );
    }

    public SessionResponse cancelSession(Long userId, Long sessionId) {
        InterviewSession session = getOwnedSession(userId, sessionId);
        validateEditable(session);
        boolean hasAnswer = interviewMessageRepository
                .existsBySessionQuestion_Session_SessionIdAndMessageTypeIn(
                        sessionId,
                        List.of(
                                InterviewMessageType.ORIGINAL_ANSWER,
                                InterviewMessageType.FOLLOW_UP_ANSWER
                        )
                );
        if (hasAnswer) {
            throw new CustomException(ErrorCode.SESSION_CANNOT_BE_CANCELED);
        }
        session.cancel();
        return SessionResponse.from(
                session,
                Math.toIntExact(sessionQuestionRepository.countBySession_SessionId(sessionId))
        );
    }

    private void validateCompanyFitSelection(
            Long userId,
            QuestionSet questionSet,
            List<Long> selectedIds
    ) {
        if (questionSet.getSnapshot() == null || questionSet.getSnapshot().getAnalysisCase() == null) {
            throw new CustomException(ErrorCode.QUESTION_SET_NOT_READY);
        }
        CompanyFitQuestionSetResponse response =
                companyFitQuestionSetQueryService.getCompanyFitQuestionSet(
                        userId,
                        questionSet.getSnapshot().getAnalysisCase().getAnalysisCaseId()
                );
        if (!response.isCanGenerateQuestions()
                || !Objects.equals(response.getQuestionSetId(), questionSet.getQuestionSetId())) {
            throw new CustomException(ErrorCode.QUESTION_SET_NOT_READY);
        }
        Set<Long> availableIds = response.getQuestions().stream()
                .map(CompanyFitQuestionSetResponse.Question::getQuestionId)
                .collect(Collectors.toSet());
        if (!availableIds.containsAll(selectedIds)) {
            throw new CustomException(ErrorCode.INVALID_QUESTION_SELECTION);
        }
    }

    private InterviewMessage resolveParentQuestion(
            Long userId,
            InterviewSessionQuestion sessionQuestion,
            AnswerSubmitRequest request
    ) {
        List<InterviewMessage> messages = interviewMessageRepository
                .findBySessionQuestion_SessionQuestionIdOrderByMessageIdAsc(
                        sessionQuestion.getSessionQuestionId()
                );
        if (request.getAnswerType() == AnswerType.ORIGINAL_ANSWER) {
            return messages.stream()
                    .filter(message -> message.getMessageType() == InterviewMessageType.ORIGINAL_QUESTION)
                    .findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_MESSAGE_NOT_FOUND));
        }
        if (request.getParentQuestionMessageId() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        InterviewMessage parent = interviewMessageRepository
                .findByMessageIdAndSessionQuestion_Session_User_UserId(
                        request.getParentQuestionMessageId(),
                        userId
                )
                .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_MESSAGE_NOT_FOUND));
        if (!Objects.equals(
                parent.getSessionQuestion().getSessionQuestionId(),
                sessionQuestion.getSessionQuestionId()
        ) || parent.getMessageType() != InterviewMessageType.FOLLOW_UP_QUESTION) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return parent;
    }

    private boolean hasUnansweredFollowUp(Long sessionId) {
        return sessionQuestionRepository.findBySession_SessionIdOrderByDisplayOrderAsc(sessionId)
                .stream()
                .map(question -> interviewMessageRepository
                        .findBySessionQuestion_SessionQuestionIdOrderByMessageIdAsc(
                                question.getSessionQuestionId()
                        ))
                .filter(messages -> !messages.isEmpty())
                .map(messages -> messages.get(messages.size() - 1))
                .anyMatch(message -> message.getMessageType() == InterviewMessageType.FOLLOW_UP_QUESTION);
    }

    private InterviewSession getOwnedSession(Long userId, Long sessionId) {
        return interviewSessionRepository
                .findBySessionIdAndUser_UserIdAndDeletedAtIsNull(sessionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));
    }

    private void validateEditable(InterviewSession session) {
        if (!session.isEditable()) {
            throw new CustomException(ErrorCode.SESSION_NOT_EDITABLE);
        }
    }
}
