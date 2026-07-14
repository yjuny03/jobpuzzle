package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewMessageRepository extends JpaRepository<InterviewMessage, Long> {
}
