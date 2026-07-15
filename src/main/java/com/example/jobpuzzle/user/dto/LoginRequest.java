package com.example.jobpuzzle.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// 로그인 화면
@Getter
@NoArgsConstructor
public class LoginRequest {

    // 로그인 아이디
    private String loginId;

    // 비밀번호
    private String password;
}
