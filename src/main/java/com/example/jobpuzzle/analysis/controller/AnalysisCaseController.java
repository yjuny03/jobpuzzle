package com.example.jobpuzzle.analysis.controller;

import com.example.jobpuzzle.analysis.dto.AnalysisCaseCreateRequest;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseJobCategoryUpdateRequest;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseResponse;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseSourceAddRequest;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseSourceResponse;
import com.example.jobpuzzle.analysis.service.AnalysisCaseService;
import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AnalysisCaseController {

    private final AnalysisCaseService analysisCaseService;

    // 분석 작업 생성 (jobCategoryId 없으면 회원 기본 관심 직무 사용)
    // POST /api/analysis-cases { jobCategoryId? }
    @PostMapping("/api/analysis-cases")
    public ResponseEntity<ApiResponse<AnalysisCaseResponse>> createCase(
            @RequestBody(required = false) AnalysisCaseCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AnalysisCaseResponse response = analysisCaseService.createCase(
                userDetails.getUser().getUserId(),
                request != null ? request : new AnalysisCaseCreateRequest()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 분석 작업 상세(연결된 자료 목록 포함) 조회
    // GET /api/analysis-cases/{analysisCaseId}
    @GetMapping("/api/analysis-cases/{analysisCaseId}")
    public ResponseEntity<ApiResponse<AnalysisCaseResponse>> getCase(
            @PathVariable Long analysisCaseId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AnalysisCaseResponse response =
                analysisCaseService.getCase(userDetails.getUser().getUserId(), analysisCaseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 분석 기준 직무·경력 변경 (DRAFT 상태에서만 가능)
    // PATCH /api/analysis-cases/{analysisCaseId}/job-category { jobCategoryId }
    @PatchMapping("/api/analysis-cases/{analysisCaseId}/job-category")
    public ResponseEntity<ApiResponse<AnalysisCaseResponse>> changeJobCategory(
            @PathVariable Long analysisCaseId,
            @RequestBody AnalysisCaseJobCategoryUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AnalysisCaseResponse response = analysisCaseService.changeJobCategory(
                userDetails.getUser().getUserId(), analysisCaseId, request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // CONFIRMED 추출본을 분석 작업에 연결
    // POST /api/analysis-cases/{analysisCaseId}/sources { extractionId }
    @PostMapping("/api/analysis-cases/{analysisCaseId}/sources")
    public ResponseEntity<ApiResponse<AnalysisCaseSourceResponse>> addSource(
            @PathVariable Long analysisCaseId,
            @RequestBody AnalysisCaseSourceAddRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        AnalysisCaseSourceResponse response = analysisCaseService.addSource(
                userDetails.getUser().getUserId(), analysisCaseId, request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 연결된 자료 제거
    // DELETE /api/analysis-cases/{analysisCaseId}/sources/{analysisCaseSourceId}
    @DeleteMapping("/api/analysis-cases/{analysisCaseId}/sources/{analysisCaseSourceId}")
    public ResponseEntity<Void> removeSource(
            @PathVariable Long analysisCaseId,
            @PathVariable Long analysisCaseSourceId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        analysisCaseService.removeSource(
                userDetails.getUser().getUserId(), analysisCaseId, analysisCaseSourceId
        );
        return ResponseEntity.noContent().build();
    }
}