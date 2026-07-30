package com.example.jobpuzzle.global.security;

import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 인증이 없을 때 화면은 로그인으로 보내고 기능 요청은 JSON 401로 반환한다. */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final LoginUrlAuthenticationEntryPoint loginEntryPoint =
            new LoginUrlAuthenticationEntryPoint("/login");

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException, ServletException {
        if (!SecurityRequestClassifier.isFunctionRequest(request)) {
            loginEntryPoint.commence(request, response, authenticationException);
            return;
        }

        response.setStatus(ErrorCode.AUTHENTICATION_REQUIRED.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ErrorCode.AUTHENTICATION_REQUIRED)));
    }
}
