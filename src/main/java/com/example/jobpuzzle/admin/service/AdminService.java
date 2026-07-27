package com.example.jobpuzzle.admin.service;

import com.example.jobpuzzle.admin.dto.AdminUserListResponse;
import com.example.jobpuzzle.admin.dto.AdminUserSearchField;
import com.example.jobpuzzle.global.common.dto.PageResponse;
import com.example.jobpuzzle.user.entity.UserStatus;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;

    // 회원 목록/검색
    public PageResponse<AdminUserListResponse> getUserList(
            String keyword, AdminUserSearchField searchField, UserStatus status, Pageable pageable
    ) {
        String searchFieldName = keyword != null ? searchField.name() : null;
        Page<AdminUserListResponse> response = userRepository.search(keyword, searchFieldName, status, pageable)
                .map(AdminUserListResponse::from);
        return PageResponse.from(response);
    }

    public void manageJobCategory() {
        // TODO: 클래스 정의서 기준으로 구현
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