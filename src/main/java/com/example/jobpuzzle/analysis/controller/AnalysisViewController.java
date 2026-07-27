package com.example.jobpuzzle.analysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 분석 결과 화면 전용 라우팅.
 * JSON API와 같은 /api/analysis 경계를 사용하되, /cases 하위의 REST API와는 분리한다.
 */
@Controller
@RequestMapping("/api/analysis")
public class AnalysisViewController {

    @GetMapping("/{analysisCaseId}")
    public String result(@PathVariable Long analysisCaseId, Model model) {
        model.addAttribute("analysisCaseId", analysisCaseId);
        return "analysis";
    }
}
