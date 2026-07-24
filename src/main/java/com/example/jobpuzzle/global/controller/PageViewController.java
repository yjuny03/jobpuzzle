package com.example.jobpuzzle.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// templates/ 밑에 있는 화면들을 그대로 렌더링하는 임시 라우팅.
// 각 화면 담당자가 별도 컨트롤러를 만들면 그쪽으로 옮기면 됨 - 지금은 라우팅 자체가 없어서 임시로 모아둠.
// {page}는 아래 화이트리스트로만 제한해서, 존재하지 않는 임의의 경로가 템플릿 이름으로 들어가지 않게 함.
@Controller
public class PageViewController {

    @GetMapping("/{page:index|dashboard|my-data|job-analysis|interview|interview-result|reports|settings|job-category-setup}.html")
    public String page(@PathVariable String page) {
        return page;
    }

    @GetMapping("/analysis/{analysisCaseId}")
    public String analysisResult(@PathVariable Long analysisCaseId) {
        return "analysis";
    }
}
