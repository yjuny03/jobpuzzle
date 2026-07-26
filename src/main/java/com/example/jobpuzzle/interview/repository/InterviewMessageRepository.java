package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewMessage;
import com.example.jobpuzzle.interview.entity.InterviewMessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {

    Optional<InterviewMessage> findBySessionQuestionIdAndMessageType(Long sessionQuestionId, InterviewMessageType messageType);
}
