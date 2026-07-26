package com.example.jobpuzzle.evaluation.repository;

import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnswerEvaluationRepository extends JpaRepository<AnswerEvaluation, Long> {

    Optional<AnswerEvaluation> findByAnswerMessageId(Long answerMessageId);
}
