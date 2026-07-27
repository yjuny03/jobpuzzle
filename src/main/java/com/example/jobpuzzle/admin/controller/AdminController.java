package com.example.jobpuzzle.admin.controller;

import com.example.jobpuzzle.admin.dto.AdminUserListResponse;
import com.example.jobpuzzle.admin.dto.AdminUserSearchField;
import com.example.jobpuzzle.admin.dto.JobCategoryCreateRequest;
import com.example.jobpuzzle.admin.service.AdminService;
import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.common.dto.PageResponse;
import com.example.jobpuzzle.jobcategory.dto.JobCategoryResponse;
import com.example.jobpuzzle.user.entity.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    // 회원 목록/검색
    // GET /api/admin/users?keyword=&searchField=LOGIN_ID&status=&page=&size=
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
}