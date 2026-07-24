package com.example.jobpuzzle.interview.controller;

import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import com.example.jobpuzzle.interview.dto.*;
import com.example.jobpuzzle.interview.service.InterviewSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;

    @GetMapping("/api/interview-modes/availability")
    public ResponseEntity<ApiResponse<InterviewModeAvailabilityResponse>> getAvailableModes(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.getAvailableModes(user.getUser().getUserId())
        ));
    }

    @PostMapping("/api/interview-sessions")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody SessionCreateRequest request
    ) {
        SessionResponse response = interviewSessionService.createSession(
                user.getUser().getUserId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/interview-sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.getSession(user.getUser().getUserId(), sessionId)
        ));
    }

    @PostMapping("/api/interview-sessions/{sessionId}/start")
    public ResponseEntity<ApiResponse<SessionResponse>> startSession(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.startSession(user.getUser().getUserId(), sessionId)
        ));
    }

    @GetMapping("/api/interview-sessions/{sessionId}/questions")
    public ResponseEntity<ApiResponse<List<SessionQuestionResponse>>> getQuestions(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.getSessionQuestions(user.getUser().getUserId(), sessionId)
        ));
    }

    @GetMapping("/api/interview-sessions/{sessionId}/questions/next")
    public ResponseEntity<ApiResponse<SessionQuestionResponse>> getNextQuestion(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.getNextQuestion(user.getUser().getUserId(), sessionId)
        ));
    }

    @PostMapping("/api/interview-session-questions/{sessionQuestionId}/answers")
    public ResponseEntity<ApiResponse<AnswerSubmitResponse>> submitAnswer(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long sessionQuestionId,
            @Valid @RequestBody AnswerSubmitRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.submitAnswer(
                        user.getUser().getUserId(),
                        sessionQuestionId,
                        request
                )
        ));
    }

    @PostMapping("/api/interview-sessions/{sessionId}/complete")
    public ResponseEntity<ApiResponse<SessionResponse>> completeSession(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.completeSession(user.getUser().getUserId(), sessionId)
        ));
    }

    @PostMapping("/api/interview-sessions/{sessionId}/cancel")
    public ResponseEntity<ApiResponse<SessionResponse>> cancelSession(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interviewSessionService.cancelSession(user.getUser().getUserId(), sessionId)
        ));
    }
}
