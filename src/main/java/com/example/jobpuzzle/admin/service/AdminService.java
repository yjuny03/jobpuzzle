package com.example.jobpuzzle.admin.service;

import com.example.jobpuzzle.admin.dto.AdminUserListResponse;
import com.example.jobpuzzle.admin.dto.AdminUserSearchField;
import com.example.jobpuzzle.admin.dto.JobCategoryCreateRequest;
import com.example.jobpuzzle.global.common.dto.PageResponse;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.jobcategory.dto.JobCategoryResponse;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.UserStatus;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final JobCategoryRepository jobCategoryRepository;

    // 회원 목록/검색
    public PageResponse<AdminUserListResponse> getUserList(
            String keyword, AdminUserSearchField searchField, UserStatus status, Pageable pageable
    ) {
        String searchFieldName = keyword != null ? searchField.name() : null;
        Page<AdminUserListResponse> response = userRepository.search(keyword, searchFieldName, status, pageable)
                .map(AdminUserListResponse::from);
        return PageResponse.from(response);
    }

    // 직무 분류 목록 조회
    public List<JobCategoryResponse> getJobCategories() {
        return jobCategoryRepository.findAll().stream()
                .map(JobCategoryResponse::from)
                .toList();
    }

    // 직무 분류 등록
    public JobCategoryResponse createJobCategory(JobCategoryCreateRequest request) {
        jobCategoryRepository.findByMainCategoryAndSubCategoryAndCareerLevel(
                request.getMainCategory(), request.getSubCategory(), request.getCareerLevel()
        ).ifPresent(existing -> { throw new CustomException(ErrorCode.JOB_CATEGORY_DUPLICATE); });

        JobCategory jobCategory = JobCategory.builder()
                .mainCategory(request.getMainCategory())
                .subCategory(request.getSubCategory())
                .careerLevel(request.getCareerLevel())
                .build();
        return JobCategoryResponse.from(jobCategoryRepository.save(jobCategory));
    }

    // 직무 분류 수정
    public JobCategoryResponse updateJobCategory(Long jobCategoryId, JobCategoryCreateRequest request) {
        JobCategory jobCategory = jobCategoryRepository.findById(jobCategoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND));

        if (jobCategoryRepository.existsByMainCategoryAndSubCategoryAndCareerLevelAndJobCategoryIdNot(
                request.getMainCategory(), request.getSubCategory(), request.getCareerLevel(), jobCategoryId)) {
            throw new CustomException(ErrorCode.JOB_CATEGORY_DUPLICATE);
        }

        jobCategory.update(request.getMainCategory(), request.getSubCategory(), request.getCareerLevel());
        return JobCategoryResponse.from(jobCategory);
    }

    // 직무 분류 삭제 - 공고·가이드·리포트 등 다른 데이터에서 참조 중이면 삭제할 수 없음
    public void deleteJobCategory(Long jobCategoryId) {
        JobCategory jobCategory = jobCategoryRepository.findById(jobCategoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND));
        try {
            jobCategoryRepository.delete(jobCategory);
            jobCategoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.JOB_CATEGORY_IN_USE);
        }
    }

    public void manageQuestionTemplate() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void getPostingCountByCategory() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void fixMisclassifiedPosting() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void manageReportDraft() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void getGuideUsageHistory() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void getAiErrorLog() {
        // TODO: 클래스 정의서 기준으로 구현
    }
}