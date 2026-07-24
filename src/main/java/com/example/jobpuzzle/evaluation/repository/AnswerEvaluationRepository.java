package com.example.jobpuzzle.evaluation.repository;

import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerEvaluationRepository extends JpaRepository<AnswerEvaluation, Long> {
    Optional<AnswerEvaluation> findByAnswerMessage_MessageId(Long answerMessageId);
    Optional<AnswerEvaluation> findByEvaluationIdAndSessionQuestion_Session_User_UserId(Long evaluationId, Long userId);
    List<AnswerEvaluation> findBySessionQuestion_SessionQuestionIdOrderByEvaluationIdAsc(Long sessionQuestionId);
    List<AnswerEvaluation> findBySessionQuestion_Session_SessionIdOrderByEvaluationIdAsc(Long sessionId);
}
