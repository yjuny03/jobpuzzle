package com.example.jobpuzzle.report.repository;

import com.example.jobpuzzle.report.entity.ImprovementSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImprovementSuggestionRepository extends JpaRepository<ImprovementSuggestion, Long> {

    List<ImprovementSuggestion> findByReportIdOrderByTargetTypeAscDisplayOrderAsc(Long reportId);
}
