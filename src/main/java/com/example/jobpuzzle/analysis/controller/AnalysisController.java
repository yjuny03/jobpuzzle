package com.example.jobpuzzle.analysis.controller;

import com.example.jobpuzzle.analysis.dto.AnalysisResultResponse;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseResponse;
import com.example.jobpuzzle.analysis.dto.AnalysisStatusResponse;
import com.example.jobpuzzle.analysis.dto.CompanyFitQuestionSetResponse;
import com.example.jobpuzzle.analysis.service.AnalysisCaseService;
import com.example.jobpuzzle.analysis.service.AnalysisResultQueryService;
import com.example.jobpuzzle.analysis.service.AnalysisRunDispatcher;
import com.example.jobpuzzle.analysis.service.AnalysisStatusQueryService;
import com.example.jobpuzzle.analysis.service.CompanyFitQuestionSetQueryService;
import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analysis")
public class AnalysisController {
    private final AnalysisRunDispatcher analysisRunDispatcher;
    private final AnalysisCaseService analysisCaseService;
    private final AnalysisResultQueryService queryService;
    private final AnalysisStatusQueryService statusQueryService;
    private final CompanyFitQuestionSetQueryService questionSetQueryService;

    // 분석은 백그라운드에서 실행하고 바로 202로 응답한다. 진행 상황과 결과는 /status로 확인한다.
    @PostMapping("/cases/{analysisCaseId}/run")
    public ResponseEntity<ApiResponse<AnalysisCaseResponse>> run(@PathVariable Long analysisCaseId, @AuthenticationPrincipal(expression = "user") User user) {
        analysisRunDispatcher.dispatch(user.getUserId(), analysisCaseId);
        return ResponseEntity.accepted().body(ApiResponse.success(analysisCaseService.getCase(user.getUserId(), analysisCaseId)));
    }

    @GetMapping("/cases/{analysisCaseId}/result")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> result(@PathVariable Long analysisCaseId, @AuthenticationPrincipal(expression = "user") User user) {
        return ResponseEntity.ok(ApiResponse.success(queryService.getResult(user.getUserId(), analysisCaseId)));
    }

    @GetMapping("/cases/{analysisCaseId}/status")
    public ResponseEntity<ApiResponse<AnalysisStatusResponse>> status(@PathVariable Long analysisCaseId, @AuthenticationPrincipal(expression = "user") User user) {
        return ResponseEntity.ok(ApiResponse.success(statusQueryService.getStatus(user.getUserId(), analysisCaseId)));
    }

    @GetMapping("/cases/{analysisCaseId}/question-set")
    public ResponseEntity<ApiResponse<CompanyFitQuestionSetResponse>> questionSet(@PathVariable Long analysisCaseId, @AuthenticationPrincipal(expression = "user") User user) {
        return ResponseEntity.ok(ApiResponse.success(questionSetQueryService.getCompanyFitQuestionSet(user.getUserId(), analysisCaseId)));
    }
}
