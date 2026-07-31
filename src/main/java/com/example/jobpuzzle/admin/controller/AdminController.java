package com.example.jobpuzzle.admin.controller;

import com.example.jobpuzzle.admin.dto.AdminAiCallLogDetailResponse;
import com.example.jobpuzzle.admin.dto.AdminAiCallLogResponse;
import com.example.jobpuzzle.admin.dto.AdminGuideCreateRequest;
import com.example.jobpuzzle.admin.dto.AdminGuideResponse;
import com.example.jobpuzzle.admin.dto.AdminGuideUsageResponse;
import com.example.jobpuzzle.admin.dto.AdminUserListResponse;
import com.example.jobpuzzle.admin.dto.AdminUserSearchField;
import com.example.jobpuzzle.admin.dto.JobCategoryCreateRequest;
import com.example.jobpuzzle.admin.service.AdminService;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.common.dto.PageResponse;
import com.example.jobpuzzle.jobcategory.dto.JobCategoryResponse;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api")
public class AdminController {

    private final AdminService adminService;

    // 회원 목록/검색
    // GET /jobpuzzle/admin-api/users?keyword=&searchField=LOGIN_ID&status=&page=&size=
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserListResponse>>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LOGIN_ID") AdminUserSearchField searchField,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable
    ) {
        PageResponse<AdminUserListResponse> response = adminService.getUserList(keyword, searchField, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 직무 분류 목록 조회
    @GetMapping("/job-categories")
    public ResponseEntity<ApiResponse<List<JobCategoryResponse>>> getJobCategories() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getJobCategories()));
    }

    // 직무 분류 등록
    @PostMapping("/job-categories")
    public ResponseEntity<ApiResponse<JobCategoryResponse>> createJobCategory(
            @Valid @RequestBody JobCategoryCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.createJobCategory(request)));
    }

    // 직무 분류 수정
    @PutMapping("/job-categories/{jobCategoryId}")
    public ResponseEntity<ApiResponse<JobCategoryResponse>> updateJobCategory(
            @PathVariable Long jobCategoryId,
            @Valid @RequestBody JobCategoryCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateJobCategory(jobCategoryId, request)));
    }

    // 직무 분류 삭제
    @DeleteMapping("/job-categories/{jobCategoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteJobCategory(@PathVariable Long jobCategoryId) {
        adminService.deleteJobCategory(jobCategoryId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 가이드 사용 이력 조회
    // GET /jobpuzzle/admin-api/guide-usage?page=&size=
    @GetMapping("/guide-usage")
    public ResponseEntity<ApiResponse<PageResponse<AdminGuideUsageResponse>>> getGuideUsageHistory(
            @PageableDefault(sort = "guideContextResultId", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getGuideUsageHistory(pageable)));
    }

    // AI 분석 오류 로그 조회
    // GET /jobpuzzle/admin-api/ai-call-logs?status=&stage=&page=&size=
    @GetMapping("/ai-call-logs")
    public ResponseEntity<ApiResponse<PageResponse<AdminAiCallLogResponse>>> getAiErrorLogs(
            @RequestParam(required = false) AiCallLogStatus status,
            @RequestParam(required = false) AiExecutionStage stage,
            @PageableDefault(sort = "aiCallLogId", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAiErrorLogs(status, stage, pageable)));
    }

    // AI 분석 오류 로그 단건 조회
    @GetMapping("/ai-call-logs/{aiCallLogId}")
    public ResponseEntity<ApiResponse<AdminAiCallLogDetailResponse>> getAiErrorLog(@PathVariable Long aiCallLogId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAiErrorLog(aiCallLogId)));
    }

    // 가이드 목록 조회
    @GetMapping("/guides")
    public ResponseEntity<ApiResponse<List<AdminGuideResponse>>> getGuides() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getGuides()));
    }

    // 가이드 단건 조회
    @GetMapping("/guides/{guideId}")
    public ResponseEntity<ApiResponse<AdminGuideResponse>> getGuide(@PathVariable Long guideId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getGuide(guideId)));
    }

    // 가이드 등록 - file은 sourceType=PDF일 때만 전달
    // POST /jobpuzzle/admin-api/guides (multipart: request(json), file(선택))
    @PostMapping(value = "/guides", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminGuideResponse>> createGuide(
            @Valid @RequestPart("request") AdminGuideCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal(expression = "user") User admin
    ) {
        AdminGuideResponse response = adminService.createGuide(request, file, admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 가이드 새 버전 생성 - guideCode는 기존 가이드에서 상속, 버전만 자동 증가
    // POST /jobpuzzle/admin-api/guides/{guideId}/versions (multipart: request(json), file(선택))
    @PostMapping(value = "/guides/{guideId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminGuideResponse>> createGuideVersion(
            @PathVariable Long guideId,
            @Valid @RequestPart("request") AdminGuideCreateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal(expression = "user") User admin
    ) {
        AdminGuideResponse response = adminService.createGuideVersion(guideId, request, file, admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 활성 가이드를 분석 대상에서 제외한다.
    @PatchMapping("/guides/{guideId}/deactivate")
    public ResponseEntity<ApiResponse<AdminGuideResponse>> deactivateGuide(
            @PathVariable Long guideId,
            @AuthenticationPrincipal(expression = "user") User admin
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.deactivateGuide(guideId, admin)));
    }
}
