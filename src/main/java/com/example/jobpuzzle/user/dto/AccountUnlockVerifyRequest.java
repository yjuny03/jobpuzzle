package com.example.jobpuzzle.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 계정 잠금 해제 - 인증 코드 검증 요청 (검증 성공 시 그 자리에서 잠금 해제까지 처리됨)
@Getter
@NoArgsConstructor
public class AccountUnlockVerifyRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String loginId;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotNull(message = "인증 코드를 입력해주세요.")
    private Integer code;
}