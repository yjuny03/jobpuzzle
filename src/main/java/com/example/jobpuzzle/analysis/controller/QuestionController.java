package com.example.jobpuzzle.analysis.controller;

import com.example.jobpuzzle.analysis.service.QuestionGenerationService;
import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.interview.dto.QuestionHintResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionGenerationService questionGenerationService;

    @GetMapping("/{questionId}/hint")
    public ResponseEntity<ApiResponse<QuestionHintResponse>> getHint(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long questionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                questionGenerationService.getQuestionHint(
                        user.getUserId(),
                        questionId
                )
        ));
    }
}
