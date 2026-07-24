package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InterviewSessionQuestionRepository extends JpaRepository<InterviewSessionQuestion, Long> {
    List<InterviewSessionQuestion> findBySession_SessionIdOrderByDisplayOrderAsc(Long sessionId);
    Optional<InterviewSessionQuestion> findBySessionQuestionIdAndSession_User_UserId(Long sessionQuestionId, Long userId);
    Optional<InterviewSessionQuestion> findFirstBySession_SessionIdAndStatusInOrderByDisplayOrderAsc(
            Long sessionId,
            Collection<InterviewSessionQuestionStatus> statuses
    );
    long countBySession_SessionId(Long sessionId);
    long countBySession_SessionIdAndStatus(Long sessionId, InterviewSessionQuestionStatus status);
}
