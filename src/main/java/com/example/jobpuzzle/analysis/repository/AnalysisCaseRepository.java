package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisCaseRepository extends JpaRepository<AnalysisCase, Long> {

    Optional<AnalysisCase> findByAnalysisCaseIdAndUser_UserId(Long analysisCaseId, Long userId);
}