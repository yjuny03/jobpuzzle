package com.example.jobpuzzle.ai.validation;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.analysis.rag.dto.RetrievedEvidenceContextDto;
import com.example.jobpuzzle.guide.dto.GuideContextResultDto;

// JSON-05 검증은 JSON-01·02·04와 고정 retrieval 근거만 사용하고 원문 자료를 재조회하지 않는다.
public record CustomizedAnalysisValidationContext(
        JobPostingAnalysisResult jobPostingAnalysis,
        CandidateMaterialAnalysisResult candidateMaterialAnalysis,
        GuideContextResultDto guideContext,
        RetrievedEvidenceContextDto retrievedEvidence
) {
}
