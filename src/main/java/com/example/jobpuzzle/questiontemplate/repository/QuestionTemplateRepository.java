package com.example.jobpuzzle.questiontemplate.repository;

import com.example.jobpuzzle.questiontemplate.entity.QuestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionTemplateRepository extends JpaRepository<QuestionTemplate, Long> {
}
