package com.example.jobpuzzle.analysis.controller;

import com.example.jobpuzzle.analysis.dto.BasicQuestionRequest;
import com.example.jobpuzzle.analysis.dto.QuestionSetResponse;
import com.example.jobpuzzle.analysis.dto.WeaknessQuestionRequest;
import com.example.jobpuzzle.analysis.service.QuestionGenerationService;
import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import com.example.jobpuzzle.interview.dto.QuestionListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/question-sets")
public class QuestionSetController {

    private final QuestionGenerationService questionGenerationService;

    @PostMapping("/basic")
    public ResponseEntity<ApiResponse<QuestionSetResponse>> generateBasic(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody BasicQuestionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                questionGenerationService.generateBasicQuestionSet(
                        user.getUser().getUserId(),
                        request
                )
        ));
    }

    @PostMapping("/weakness")
    public ResponseEntity<ApiResponse<QuestionSetResponse>> generateWeakness(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody WeaknessQuestionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                questionGenerationService.generateWeaknessQuestionSet(
                        user.getUser().getUserId(),
                        request
                )
        ));
    }

    @GetMapping("/{questionSetId}")
    public ResponseEntity<ApiResponse<QuestionSetResponse>> getQuestionSet(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long questionSetId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                questionGenerationService.getQuestionSet(
                        user.getUser().getUserId(),
                        questionSetId
                )
        ));
    }

    @GetMapping("/{questionSetId}/questions")
    public ResponseEntity<ApiResponse<List<QuestionListResponse>>> getQuestions(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long questionSetId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                questionGenerationService.getQuestionList(
                        user.getUser().getUserId(),
                        questionSetId
                )
        ));
    }
}
