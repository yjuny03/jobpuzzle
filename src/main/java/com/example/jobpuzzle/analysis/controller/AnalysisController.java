package com.example.jobpuzzle.analysis.controller;

import com.example.jobpuzzle.analysis.dto.AnalysisResultResponse;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseResponse;
import com.example.jobpuzzle.analysis.dto.AnalysisStatusResponse;
import com.example.jobpuzzle.analysis.dto.CompanyFitQuestionSetResponse;
import com.example.jobpuzzle.analysis.service.AnalysisCaseService;
import com.example.jobpuzzle.analysis.service.AnalysisResultQueryService;
import com.example.jobpuzzle.analysis.service.AnalysisService;
import com.example.jobpuzzle.analysis.service.AnalysisStatusQueryService;
import com.example.jobpuzzle.analysis.service.CompanyFitQuestionSetQueryService;
import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisController {
    private final AnalysisService analysisService;
    private final AnalysisCaseService analysisCaseService;
    private final AnalysisResultQueryService queryService;
    private final AnalysisStatusQueryService statusQueryService;
    private final CompanyFitQuestionSetQueryService questionSetQueryService;

    @PostMapping("/cases/{analysisCaseId}/run")
    public ResponseEntity<ApiResponse<AnalysisCaseResponse>> run(@PathVariable Long analysisCaseId, @AuthenticationPrincipal CustomUserDetails user) {
        analysisService.runCustomizedAnalysis(user.getUser().getUserId(), analysisCaseId);
        return ResponseEntity.ok(ApiResponse.success(analysisCaseService.getCase(user.getUser().getUserId(), analysisCaseId)));
    }

    @GetMapping("/cases/{analysisCaseId}/result")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> result(@PathVariable Long analysisCaseId, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(queryService.getResult(user.getUser().getUserId(), analysisCaseId)));
    }

    @GetMapping("/cases/{analysisCaseId}/status")
    public ResponseEntity<ApiResponse<AnalysisStatusResponse>> status(@PathVariable Long analysisCaseId, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(statusQueryService.getStatus(user.getUser().getUserId(), analysisCaseId)));
    }

    @GetMapping("/cases/{analysisCaseId}/question-set")
    public ResponseEntity<ApiResponse<CompanyFitQuestionSetResponse>> questionSet(@PathVariable Long analysisCaseId, @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(questionSetQueryService.getCompanyFitQuestionSet(user.getUser().getUserId(), analysisCaseId)));
    }
}
