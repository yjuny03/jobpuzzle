package com.example.jobpuzzle.ai.prompt;

import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    Optional<PromptTemplate> findFirstByTargetJsonAndIsActiveTrueOrderByPromptTemplateIdDesc(String targetJson);
}
