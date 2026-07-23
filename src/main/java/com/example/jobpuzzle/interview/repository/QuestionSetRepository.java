package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.QuestionSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionSetRepository extends JpaRepository<QuestionSet, Long> {
}
