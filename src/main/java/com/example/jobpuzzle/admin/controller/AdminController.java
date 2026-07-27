package com.example.jobpuzzle.admin.controller;

import com.example.jobpuzzle.admin.dto.AdminUserListResponse;
import com.example.jobpuzzle.admin.dto.AdminUserSearchField;
import com.example.jobpuzzle.admin.service.AdminService;
import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.common.dto.PageResponse;
import com.example.jobpuzzle.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}