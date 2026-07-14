package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.ConfirmedAnalysisSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmedAnalysisSnapshotRepository extends JpaRepository<ConfirmedAnalysisSnapshot, Long> {
}
