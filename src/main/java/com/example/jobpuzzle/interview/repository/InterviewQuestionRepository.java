package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
}
