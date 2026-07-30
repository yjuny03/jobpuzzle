package com.example.jobpuzzle.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// 관리자 화면 라우팅
@Controller
public class AdminViewController {

    @GetMapping("/admin/users")
    public String users() {
        return "admin/users";
    }

    @GetMapping("/admin/job-categories")
    public String jobCategories() {
        return "admin/job-categories";
    }

    @GetMapping("/admin/guide-usage")
    public String guideUsage() {
        return "admin/guide-usage";
    }

    @GetMapping("/admin/ai-logs")
    public String aiLogs() {
        return "admin/ai-logs";
    }

    @GetMapping("/admin/guides")
    public String guides() {
        return "admin/guides";
    }

    @GetMapping("/admin/guides/{guideId}")
    public String guideDetail(@PathVariable Long guideId) {
        return "admin/guide-detail";
    }
}
