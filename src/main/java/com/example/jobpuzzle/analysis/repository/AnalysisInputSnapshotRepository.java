package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisInputSnapshotRepository extends JpaRepository<AnalysisInputSnapshot, Long> {

    Optional<AnalysisInputSnapshot> findByAnalysisCase_AnalysisCaseIdAndUser_UserId(Long analysisCaseId, Long userId);

    void deleteByUser_UserId(Long userId);
}