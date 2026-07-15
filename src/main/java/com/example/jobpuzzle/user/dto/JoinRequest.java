package com.example.jobpuzzle.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// 회원가입
@Getter
@NoArgsConstructor
public class JoinRequest {

    // 아이디
    private String loginId;

    // 비밀번호
    private String password;

    // 이메일
    private String email;

    // 이름
    private String name;
}
