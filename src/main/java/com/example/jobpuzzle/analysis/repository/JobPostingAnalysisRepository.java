package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.JobPostingAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingAnalysisRepository extends JpaRepository<JobPostingAnalysis, Long> {
}
