package com.example.jobpuzzle.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 로그인/회원가입 화면(Thymeleaf 뷰) 라우팅
@Controller
public class AuthViewController {

    // GET /login -> templates/login.html
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // GET /join -> templates/join.html
    @GetMapping("/join")
    public String joinPage() {
        return "join";
    }
}
