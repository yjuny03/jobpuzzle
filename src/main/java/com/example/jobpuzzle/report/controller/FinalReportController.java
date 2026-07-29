package com.example.jobpuzzle.report.controller;

import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import com.example.jobpuzzle.report.dto.FinalReportResponse;
import com.example.jobpuzzle.report.service.FinalReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/final-report")
public class FinalReportController {

    private final FinalReportService finalReportService;

    // 저장된 리포트가 있으면 그대로, 없으면 그 자리에서 생성해서 반환
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<FinalReportResponse>> getFinalReport(
            @PathVariable Long sessionId, @AuthenticationPrincipal CustomUserDetails user
    ) {
        FinalReportResponse response = finalReportService.getFinalReport(user.getUser().getUserId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
