package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewSessionQuestionRepository extends JpaRepository<InterviewSessionQuestion, Long> {

    List<InterviewSessionQuestion> findBySessionIdOrderByDisplayOrderAsc(Long sessionId);
}
