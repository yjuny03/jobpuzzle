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

    // 로그인 아이디 (일반 회원가입 시에만 사용, 소셜 로그인 회원은 null일 수 있음)
    private String loginId;

    // 암호화된 비밀번호 (일반 회원가입 시에만 사용)
    private String password;

    private String email;

    private String name;

    // 권한 - "USER", "ADMIN" 등
    private String role;

    private Long defaultJobCategoryId;

    // 소셜 로그인 제공자 - "kakao", "google" 등 (일반 회원가입 회원은 null)
    private String socialProvider;

    // 소셜 로그인 제공자가 발급한 회원 고유 ID
    private String socialId;

    // 로그인 실패 누적 횟수 - 일정 횟수 넘으면 계정 잠금
    private Integer loginFailCount;

    // 계정 잠금 여부
    private Boolean isLocked;

    private LocalDateTime lockedAt;

    // 회원 상태 - "ACTIVE"(정상), "WITHDRAWN"(탈퇴) 등
    private String status;

    private LocalDateTime withdrawnAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    private User(String loginId, String password, String email, String name, String role,
                  String socialProvider, String socialId, Integer loginFailCount,
                  Boolean isLocked, String status) {
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
                .role("USER")
                .socialProvider(socialProvider)
                .socialId(socialId)
                .loginFailCount(0)
                .isLocked(false)
                .status("ACTIVE")
                .build();
    }

    // 카카오 로그인 시 이름/이메일이 바뀐 경우 최신 정보로 갱신
    public void updateSocialProfile(String email, String name) {
        this.email = email;
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    // 아이디/비밀번호로 처음 가입하는 회원 생성 (password는 암호화된 값이어야 함)
    public static User createLocalUser(String loginId, String encodedPassword, String email, String name) {
        return User.builder()
                .loginId(loginId)
                .password(encodedPassword)
                .email(email)
                .name(name)
                .role("USER")
                .loginFailCount(0)
                .isLocked(false)
                .status("ACTIVE")
                .build();
    }

    // 로그인 실패 시 호출 - 실패 횟수가 쌓이면 계정을 잠금
    public void increaseLoginFailCount() {
        this.loginFailCount = (this.loginFailCount == null ? 0 : this.loginFailCount) + 1;
        if (this.loginFailCount >= MAX_LOGIN_FAIL_COUNT) {
            this.isLocked = true;
            this.lockedAt = LocalDateTime.now();
        }
    }

    // 로그인 성공 시 호출 - 실패 횟수 초기화
    public void resetLoginFailCount() {
        this.loginFailCount = 0;
    }
}
