package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.interview.entity.InterviewSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    Optional<InterviewSession> findBySessionIdAndUser_UserIdAndDeletedAtIsNull(Long sessionId, Long userId);

    // 면접 기능 추가: 하나의 QuestionSet에 동시에 하나의 미완료 세션만 허용한다.
    boolean existsByQuestionSet_QuestionSetIdAndStatusIn(
            Long questionSetId,
            Collection<InterviewSessionStatus> statuses
    );
}
