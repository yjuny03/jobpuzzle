package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.MatchAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchAnalysisResultRepository extends JpaRepository<MatchAnalysisResult, Long> {
}
