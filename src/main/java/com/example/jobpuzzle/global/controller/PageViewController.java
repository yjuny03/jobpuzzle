package com.example.jobpuzzle.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 사용자 화면 URL은 확장자를 노출하지 않고 템플릿 이름만 반환한다.
@Controller
public class PageViewController {

    @GetMapping({"", "/"})
    public String index() { return "index"; }

    @GetMapping("/dashboard")
    public String dashboard() { return "dashboard"; }

    @GetMapping("/my-data")
    public String myData() { return "my-data"; }

    @GetMapping("/job-analysis")
    public String jobAnalysis() { return "job-analysis"; }

    @GetMapping("/interview")
    public String interview() { return "interview"; }

    @GetMapping("/interview-results")
    public String interviewResult() { return "interview-result"; }

    @GetMapping("/reports")
    public String reports() { return "reports"; }

    @GetMapping("/settings")
    public String settings() { return "settings"; }

    @GetMapping("/job-category-setup")
    public String jobCategorySetup() { return "job-category-setup"; }
}
