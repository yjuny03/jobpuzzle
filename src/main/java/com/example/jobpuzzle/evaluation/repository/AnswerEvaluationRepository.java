package com.example.jobpuzzle.evaluation.repository;

import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerEvaluationRepository
        extends JpaRepository<AnswerEvaluation, Long> {

    // 실제 JPA 연관관계 기준 조회
    Optional<AnswerEvaluation> findByAnswerMessage_MessageId(
            Long answerMessageId
    );

    Optional<AnswerEvaluation>
    findByEvaluationIdAndSessionQuestion_Session_User_UserId(
            Long evaluationId,
            Long userId
    );

    List<AnswerEvaluation>
    findBySessionQuestion_SessionQuestionIdOrderByEvaluationIdAsc(
            Long sessionQuestionId
    );

    List<AnswerEvaluation>
    findBySessionQuestion_Session_SessionIdOrderByEvaluationIdAsc(
            Long sessionId
    );

    // 최종 리포트 팀 코드가 기존 이름으로 호출할 수 있도록 만든 호환 메서드
    default Optional<AnswerEvaluation> findByAnswerMessageId(
            Long answerMessageId
    ) {
        return findByAnswerMessage_MessageId(answerMessageId);
    }
}