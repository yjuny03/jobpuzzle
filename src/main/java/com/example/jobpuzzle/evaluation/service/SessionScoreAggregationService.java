package com.example.jobpuzzle.evaluation.service;

import com.example.jobpuzzle.evaluation.dto.SessionScoreSummary;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import com.example.jobpuzzle.evaluation.repository.AnswerEvaluationRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestionStatus;
import com.example.jobpuzzle.interview.repository.InterviewSessionQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionScoreAggregationService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewSessionQuestionRepository sessionQuestionRepository;
    private final AnswerEvaluationRepository answerEvaluationRepository;

    public SessionScoreSummary aggregate(Long userId, Long sessionId) {
        InterviewSession session = interviewSessionRepository
                .findBySessionIdAndUser_UserIdAndDeletedAtIsNull(sessionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));
        List<InterviewSessionQuestion> questions =
                sessionQuestionRepository.findBySession_SessionIdOrderByDisplayOrderAsc(sessionId);

        List<SessionScoreSummary.QuestionScore> questionScores = new ArrayList<>();
        for (InterviewSessionQuestion question : questions) {
            List<AnswerEvaluation> evaluations =
                    answerEvaluationRepository.findBySessionQuestion_SessionQuestionIdOrderByEvaluationIdAsc(
                            question.getSessionQuestionId()
                    );
            if (!evaluations.isEmpty()) {
                questionScores.add(questionScore(question, evaluations));
            }
        }

        Map<String, Integer> categoryScores = aggregateCategories(questionScores);
        Integer overallScore = session.getMode() == InterviewSessionMode.WEAKNESS_REVIEW
                ? null
                : roundedAverage(questionScores.stream()
                        .map(SessionScoreSummary.QuestionScore::getFinalScore)
                        .filter(Objects::nonNull)
                        .toList());
        int total = questions.size();
        int submitted = questionScores.size();
        int skipped = (int) questions.stream()
                .filter(question -> question.getStatus() == InterviewSessionQuestionStatus.SKIPPED)
                .count();

        return SessionScoreSummary.builder()
                .sessionId(sessionId)
                .mode(session.getMode())
                .overallScore(overallScore)
                .categoryScores(categoryScores)
                .totalQuestionCount(total)
                .submittedQuestionCount(submitted)
                .evaluatedQuestionCount(submitted)
                .skippedQuestionCount(skipped)
                .completionRate(total == 0 ? 0 : (int) Math.round(submitted * 100.0 / total))
                .questionScores(questionScores)
                .build();
    }

    private SessionScoreSummary.QuestionScore questionScore(
            InterviewSessionQuestion question,
            List<AnswerEvaluation> evaluations
    ) {
        Map<String, List<Integer>> dimensionValues = new LinkedHashMap<>();
        for (AnswerEvaluation evaluation : evaluations) {
            evaluation.getEvaluationDetail().forEach((dimension, detail) -> {
                if (detail.getScore() != null) {
                    dimensionValues.computeIfAbsent(dimension, ignored -> new ArrayList<>())
                            .add(detail.getScore());
                }
            });
        }
        Map<String, Integer> dimensionScores = dimensionValues.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> roundedAverage(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return SessionScoreSummary.QuestionScore.builder()
                .sessionQuestionId(question.getSessionQuestionId())
                .dimensionScores(dimensionScores)
                .finalScore(roundedAverage(new ArrayList<>(dimensionScores.values())))
                .build();
    }

    private Map<String, Integer> aggregateCategories(
            List<SessionScoreSummary.QuestionScore> questionScores
    ) {
        Map<String, List<Integer>> values = new LinkedHashMap<>();
        for (SessionScoreSummary.QuestionScore question : questionScores) {
            question.getDimensionScores().forEach((dimension, score) ->
                    values.computeIfAbsent(dimension, ignored -> new ArrayList<>()).add(score));
        }
        return values.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> roundedAverage(entry.getValue()),
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private Integer roundedAverage(List<Integer> values) {
        if (values.isEmpty()) {
            return null;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }
}
