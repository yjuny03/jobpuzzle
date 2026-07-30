package com.example.jobpuzzle.jobcategory.controller;

import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.jobcategory.dto.JobCategoryResponse;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job-category")
public class JobCategoryController {

    private final JobCategoryRepository jobCategoryRepository;

    // 회원가입 화면의 대분류/중분류/경력 드롭다운을 구성하기 위한 전체 직무 카테고리 목록
    // GET /jobpuzzle/job-category
    @GetMapping
    public ResponseEntity<ApiResponse<List<JobCategoryResponse>>> getAllJobCategories() {
        List<JobCategoryResponse> response = jobCategoryRepository.findAll().stream()
                .map(JobCategoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
