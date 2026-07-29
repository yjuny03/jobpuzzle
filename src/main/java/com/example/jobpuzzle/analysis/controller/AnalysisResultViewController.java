package com.example.jobpuzzle.analysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 완료된 분석을 다시 열어보는 읽기 전용 화면.
 * REST API 경로와 화면 URL을 분리해 브라우저 화면임을 명확히 한다.
 */
@Controller
public class AnalysisResultViewController {

    @GetMapping("/analysis-results/{analysisCaseId}")
    public String result(@PathVariable Long analysisCaseId, Model model) {
        model.addAttribute("analysisCaseId", analysisCaseId);
        return "analysis-result";
    }
}
