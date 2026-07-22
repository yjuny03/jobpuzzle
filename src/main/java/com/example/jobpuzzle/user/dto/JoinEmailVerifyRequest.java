package com.example.jobpuzzle.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원가입 - 이메일 인증 코드 검증 요청 (검증만 하고 코드는 소비하지 않음, 실제 가입 시점에 재검증됨)
@Getter
@NoArgsConstructor
public class JoinEmailVerifyRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotNull(message = "인증 코드를 입력해주세요.")
    private Integer code;
}