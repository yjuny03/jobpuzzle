package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
}
