package com.example.jobpuzzle.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 관리자 화면 라우팅
@Controller
public class AdminViewController {

    @GetMapping("/admin/users")
    public String users() {
        return "admin/users";
    }
}