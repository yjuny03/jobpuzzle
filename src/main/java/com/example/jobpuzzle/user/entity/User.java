package com.example.jobpuzzle.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String loginId;

    private String password;

    private String email;

    private String name;

    private UserRole role;

    private Long defaultJobCategoryId;

    private String socialProvider;

    private String socialId;

    private Integer loginFailCount;

    private Boolean isLocked;

    private LocalDateTime lockedAt;

    private UserStatus status;

    private LocalDateTime withdrawnAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    private User(String loginId, String password, String email, String name, UserRole role,
                  String socialProvider, String socialId, Integer loginFailCount,
                  Boolean isLocked, UserStatus status) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.name = name;
        this.role = role;
        this.socialProvider = socialProvider;
        this.socialId = socialId;
        this.loginFailCount = loginFailCount;
        this.isLocked = isLocked;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 카카오 로그인으로 처음 가입하는 회원 생성
    public static User createSocialUser(String socialProvider, String socialId, String email, String name) {
        return User.builder()
                .email(email)
                .name(name)
                .role(UserRole.USER)
                .socialProvider(socialProvider)
                .socialId(socialId)
                .loginFailCount(0)
                .isLocked(false)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // 카카오 로그인 시 이름/이메일이 바뀐 경우 최신 정보로 갱신
    public void updateSocialProfile(String email, String name) {
        this.email = email;
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }
}
