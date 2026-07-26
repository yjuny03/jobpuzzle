package com.example.jobpuzzle.evaluation.controller;

import com.example.jobpuzzle.evaluation.dto.AnswerEvaluationResponse;
import com.example.jobpuzzle.evaluation.service.AnswerEvaluationService;
import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/answer-evaluations")
public class AnswerEvaluationController {

    private final AnswerEvaluationService answerEvaluationService;

    @GetMapping("/{evaluationId}")
    public ResponseEntity<ApiResponse<AnswerEvaluationResponse>> getEvaluation(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long evaluationId
    ) {
        return ResponseEntity.ok(ApiResponse.success(AnswerEvaluationResponse.from(
                answerEvaluationService.getEvaluation(
                        user.getUser().getUserId(),
                        evaluationId
                )
        )));
    }
}
