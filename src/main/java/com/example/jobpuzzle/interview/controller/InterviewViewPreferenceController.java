package com.example.jobpuzzle.interview.controller;

import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.interview.dto.InterviewViewPreferenceRequest;
import com.example.jobpuzzle.interview.dto.InterviewViewPreferenceResponse;
import com.example.jobpuzzle.interview.service.InterviewViewPreferenceService;
import com.example.jobpuzzle.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview-view-preference")
public class InterviewViewPreferenceController {

    private final InterviewViewPreferenceService service;

    @GetMapping
    public ResponseEntity<ApiResponse<InterviewViewPreferenceResponse>> get(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                new InterviewViewPreferenceResponse(service.getSectionOrder(user.getUserId()))
        ));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<InterviewViewPreferenceResponse>> update(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody InterviewViewPreferenceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                new InterviewViewPreferenceResponse(
                        service.saveSectionOrder(user.getUserId(), request.getSectionOrder())
                )
        ));
    }
}
