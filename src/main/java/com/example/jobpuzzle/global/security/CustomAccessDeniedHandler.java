package com.example.jobpuzzle.global.security;

import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 로그인은 했지만 권한이 없는 요청 처리
// API 요청은 기존 ApiResponse 포맷 그대로 JSON 403을, 화면 요청은 메인 화면으로 리다이렉트
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (isFunctionRequest(request.getServletPath())) {
            response.setStatus(ErrorCode.ACCESS_DENIED.getStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.ACCESS_DENIED)));
            return;
        }
        response.sendRedirect(request.getContextPath() + "/");
    }

    /** `/api` 접두사 없이도 화면 요청과 기능 요청을 구분한다. */
    private boolean isFunctionRequest(String path) {
        return path.startsWith("/user/")
                || path.startsWith("/documents")
                || path.startsWith("/document-extractions")
                || path.startsWith("/analysis-cases")
                || path.startsWith("/analysis/cases")
                || path.startsWith("/question-sets")
                || path.startsWith("/questions")
                || path.startsWith("/interview-sessions")
                || path.startsWith("/interview-session-questions")
                || path.startsWith("/interview-modes")
                || path.startsWith("/interview-weakness-tags")
                || path.startsWith("/interview-history")
                || path.startsWith("/answer-evaluations")
                || path.startsWith("/action-plan")
                || path.startsWith("/job-category")
                || path.startsWith("/job-analysis-report")
                || path.startsWith("/recommendation")
                || path.startsWith("/final-report")
                || path.startsWith("/admin-api")
                || path.startsWith("/guide-admin");
    }
}
