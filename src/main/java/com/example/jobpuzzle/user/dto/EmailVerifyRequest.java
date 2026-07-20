package com.example.jobpuzzle.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 아이디 찾기 - 인증 코드 검증 요청
@Getter
@NoArgsConstructor
public class EmailVerifyRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotNull(message = "인증 코드를 입력해주세요.")
    private Integer code;
}