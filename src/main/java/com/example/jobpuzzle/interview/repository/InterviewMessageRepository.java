package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {
    List<InterviewMessage> findBySessionQuestion_SessionQuestionIdOrderByMessageIdAsc(Long sessionQuestionId);
    Optional<InterviewMessage> findByMessageIdAndSessionQuestion_Session_User_UserId(Long messageId, Long userId);
    boolean existsBySessionQuestion_Session_SessionIdAndMessageTypeIn(
            Long sessionId,
            java.util.Collection<com.example.jobpuzzle.interview.entity.InterviewMessageType> messageTypes
    );
    long countBySessionQuestion_SessionQuestionIdAndMessageType(
            Long sessionQuestionId,
            com.example.jobpuzzle.interview.entity.InterviewMessageType messageType
    );
    boolean existsByParentMessage_MessageIdAndSender(
            Long parentMessageId,
            com.example.jobpuzzle.interview.entity.InterviewMessageSender sender
    );
}
