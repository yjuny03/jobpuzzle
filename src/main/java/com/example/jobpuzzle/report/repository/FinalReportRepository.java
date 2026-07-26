package com.example.jobpuzzle.report.repository;

import com.example.jobpuzzle.report.entity.FinalReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinalReportRepository extends JpaRepository<FinalReport, Long> {

    boolean existsBySession_SessionId(Long sessionId);

    Optional<FinalReport> findBySession_SessionId(Long sessionId);
}
