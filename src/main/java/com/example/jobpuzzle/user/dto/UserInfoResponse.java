package com.example.jobpuzzle.user.dto;

import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 내 정보 조회 응답 (/api/user/me)
@Getter
@AllArgsConstructor
public class UserInfoResponse {

    private Long userId;
    private String loginId;
    private String email;
    private String name;
    private UserRole role;
    private Long defaultJobCategoryId;

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getDefaultJobCategory().getJobCategoryId()
        );
    }
}
