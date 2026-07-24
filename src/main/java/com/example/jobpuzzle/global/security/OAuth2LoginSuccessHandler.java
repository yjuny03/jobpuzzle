package com.example.jobpuzzle.global.security;

import com.example.jobpuzzle.user.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 카카오/구글 로그인 성공 후 이동할 화면 결정
// 희망 직무가 아직 없는 회원(최초 소셜 가입 포함)이면 직무 설정 화면으로, 이미 있으면 메인 화면으로 보냄
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        User user = ((CustomOAuth2User) authentication.getPrincipal()).getUser();
        String redirectUrl = user.getDefaultJobCategory() == null
                ? "/job-category-setup.html"
                : "/index.html";
        response.sendRedirect(redirectUrl);
    }
}
