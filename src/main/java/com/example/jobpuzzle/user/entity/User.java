package com.example.jobpuzzle.user.entity;

import com.example.jobpuzzle.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "user")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // 로그인 아이디 - 테이블 정의서상 NOT NULL/UNIQUE. 소셜 가입 회원은 "{provider}_{socialId}"로 자동 채움 (User.createSocialUser 참고)
    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    // 암호화된 비밀번호 (소셜 전용 계정은 NULL)
    @Column(length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    // 권한 - DB에는 문자열로 저장 (숫자 순번으로 저장하면 나중에 값 추가/순서 변경 시 데이터가 깨짐)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    // 기본 관심 직무 - 정의서상 NOT NULL이지만, 회원가입 화면에 직무 선택 UI가 아직 없어서
    // 당장은 nullable로 두고 직무 선택 기능이 만들어지면 NOT NULL로 전환하기로 함
    @Column(name = "default_job_category_id")
    private Long defaultJobCategoryId;

    // 소셜 로그인 제공자 - "kakao", "google" 등 (일반 회원가입 회원은 null)
    @Column(length = 20)
    private String socialProvider;

    // 소셜 로그인 제공자가 발급한 회원 고유 ID
    @Column(length = 100)
    private String socialId;

    // 로그인 실패 누적 횟수 - 일정 횟수 넘으면 계정 잠금
    @Column(nullable = false)
    private Integer loginFailCount;

    // 계정 잠금 여부
    @Column(nullable = false)
    private Boolean isLocked;

    private LocalDateTime lockedAt;

    // 계정 상태 - role과 같은 이유로 문자열 저장
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    private LocalDateTime withdrawnAt;

    @Builder
    private User(String loginId, String password, String email, String name, UserRole role,
                  Long defaultJobCategoryId, String socialProvider, String socialId, Integer loginFailCount,
                  Boolean isLocked, UserStatus status) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.name = name;
        this.role = role;
        this.defaultJobCategoryId = defaultJobCategoryId;
        this.socialProvider = socialProvider;
        this.socialId = socialId;
        this.loginFailCount = loginFailCount;
        this.isLocked = isLocked;
        this.status = status;
    }

    // 카카오 로그인으로 처음 가입하는 회원 생성
    // login_id는 정의서상 NOT NULL이라 소셜 회원도 "{provider}_{socialId}" 형식으로 자동 채움 (직접 로그인용으로 쓰이진 않음)
    public static User createSocialUser(String socialProvider, String socialId, String email, String name) {
        return User.builder()
                .loginId(socialProvider + "_" + socialId)
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
    }

    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    // 아이디/비밀번호로 처음 가입하는 회원 생성 (password는 암호화된 값이어야 함)
    public static User createLocalUser(String loginId, String encodedPassword, String email, String name,
                                        Long defaultJobCategoryId) {
        return User.builder()
                .loginId(loginId)
                .password(encodedPassword)
                .email(email)
                .name(name)
                .role(UserRole.USER)
                .defaultJobCategoryId(defaultJobCategoryId)
                .loginFailCount(0)
                .isLocked(false)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // 내 정보 수정 - 이름/이메일/기본 관심 직무 변경
    public void updateProfile(String name, String email, Long defaultJobCategoryId) {
        this.name = name;
        this.email = email;
        this.defaultJobCategoryId = defaultJobCategoryId;
    }

    // 회원 탈퇴 처리 - 상태만 WITHDRAWN으로 바꾸고 탈퇴 시각 기록 (실제 데이터 삭제는 안 함)
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
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
