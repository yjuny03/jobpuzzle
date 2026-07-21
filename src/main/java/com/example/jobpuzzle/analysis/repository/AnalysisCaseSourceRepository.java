package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.AnalysisCaseSource;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisCaseSourceRepository extends JpaRepository<AnalysisCaseSource, Long> {

    List<AnalysisCaseSource> findByAnalysisCase_AnalysisCaseIdOrderByAnalysisCaseSourceIdAsc(Long analysisCaseId);

    boolean existsByAnalysisCase_AnalysisCaseIdAndExtraction_ExtractionId(Long analysisCaseId, Long extractionId);

    boolean existsByAnalysisCase_AnalysisCaseIdAndDocumentType(Long analysisCaseId, UserDocumentType documentType);

    Optional<AnalysisCaseSource> findByAnalysisCaseSourceIdAndAnalysisCase_AnalysisCaseId(
            Long analysisCaseSourceId, Long analysisCaseId
    );
}