package com.example.jobpuzzle.analysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 진행 중인 분석 화면 전용 라우팅.
 * JSON 응답이 아니라 HTML을 반환하므로 API 경로와 분리한다.
 */
@Controller
public class AnalysisViewController {

    @GetMapping("/analysis/{analysisCaseId}")
    public String result(@PathVariable Long analysisCaseId, Model model) {
        model.addAttribute("analysisCaseId", analysisCaseId);
        return "analysis";
    }
}
