package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionQuestionRepository extends JpaRepository<InterviewSessionQuestion, Long> {
}
