package com.example.jobpuzzle.global.security;

import com.example.jobpuzzle.user.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 카카오/구글 로그인 성공 후 이동할 화면 결정
// 희망 직무가 아직 없는 회원(최초 소셜 가입 포함)이면 직무 설정 화면으로, 이미 있으면 메인 화면으로 보냄
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    // SecurityConfig 생성과 순환 의존하지 않으면서 같은 세션 저장 키의 원래 요청을 읽는다.
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        User user = ((CustomOAuth2User) authentication.getPrincipal()).getUser();
        // TODO(team): 카카오·구글 개발자 콘솔의 redirect URI에
        // /jobpuzzle/login/oauth2/code/{registrationId} 경로를 등록해야 한다.
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String redirectUrl;
        if (user.getDefaultJobCategory() == null) {
            redirectUrl = request.getContextPath() + "/job-category-setup";
        } else if (savedRequest != null) {
            redirectUrl = savedRequest.getRedirectUrl();
        } else {
            redirectUrl = request.getContextPath() + "/";
        }
        if (savedRequest != null) {
            requestCache.removeRequest(request, response);
        }
        response.sendRedirect(redirectUrl);
    }
}
