package com.example.jobpuzzle.admin.dto;

import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import com.example.jobpuzzle.user.entity.UserStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminUserListResponse {

    private final Long userId;
    private final String loginId;
    private final String name;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final Boolean isLocked;
    private final String socialProvider;
    private final LocalDateTime createdAt;

    private AdminUserListResponse(
            Long userId, String loginId, String name, String email,
            UserRole role, UserStatus status, Boolean isLocked, String socialProvider, LocalDateTime createdAt
    ) {
        this.userId = userId;
        this.loginId = loginId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.isLocked = isLocked;
        this.socialProvider = socialProvider;
        this.createdAt = createdAt;
    }

    public static AdminUserListResponse from(User user) {
        return new AdminUserListResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getIsLocked(),
                user.getSocialProvider(),
                user.getCreatedAt()
        );
    }
}