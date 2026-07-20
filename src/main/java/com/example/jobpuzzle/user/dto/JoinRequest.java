package com.example.jobpuzzle.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원가입 (USER 테이블 정의서 기준 - login_id 50자, email 100자, name 50자)
@Getter
@NoArgsConstructor
public class JoinRequest {

    // 아이디
    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(min = 4, max = 50, message = "아이디는 4~50자로 입력해주세요.")
    private String loginId;

    // 비밀번호 - 형식 검증은 UserService.validatePassword()에서 정책대로 별도 처리
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    // 이메일
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 100자 이내로 입력해주세요.")
    private String email;

    // 이름
    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.")
    private String name;

    // 기본 관심 직무 - 회원가입 화면에 대분류/중분류/경력 드롭다운 추가되면서 필수로 전환
    @NotNull(message = "관심 직무를 선택해주세요.")
    private Long defaultJobCategoryId;

    // 이메일 인증 코드 - /join/verify로 미리 검증했더라도 가입 처리 시점에 재검증됨
    @NotNull(message = "이메일 인증을 먼저 완료해주세요.")
    private Integer code;
}
