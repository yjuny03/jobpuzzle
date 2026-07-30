package com.example.jobpuzzle.global.security;

import com.example.jobpuzzle.user.entity.User;

// 로그인 수단(아이디/비밀번호 vs 카카오/구글)에 관계없이 로그인된 회원 엔티티를 꺼내기 위한 공통 인터페이스.
// @AuthenticationPrincipal에서 CustomUserDetails 타입만 받으면 OAuth2 로그인 시 principal이
// CustomOAuth2User라 타입이 안 맞아 null이 되던 문제 때문에 추가함 (/jobpuzzle/user/me 등에서 발생)
public interface AuthUser {
    User getUser();
}
