package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.log.AiProvider;

public interface AiClient {

    JobPostingAnalysisResult analyzeJobPosting(String prompt);

    CandidateMaterialAnalysisResult analyzeCandidateMaterial(String prompt);

    QuestionGenerationResult generateQuestions(String prompt);

    FinalReportResult finalReport(String prompt);

    AiProvider getProvider();

    String call(String prompt);
}
