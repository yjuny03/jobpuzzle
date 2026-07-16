package com.example.jobpuzzle.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그인 화면
@Getter
@NoArgsConstructor
public class LoginRequest {

    // 로그인 아이디
    @NotBlank(message = "아이디를 입력해주세요.")
    private String loginId;

    // 비밀번호
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    // 자동로그인(리멤버미) 체크 여부
    private Boolean autoLogin;
}
