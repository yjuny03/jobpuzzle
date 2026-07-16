package com.example.jobpuzzle.global.security;

import org.springframework.data.jpa.repository.JpaRepository;

// 자동로그인(리멤버미) 토큰 저장소 - JpaPersistentTokenRepository에서 사용
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    // 로그아웃/전체 무효화 시 해당 회원의 모든 자동로그인 토큰 삭제
    void deleteByUsername(String username);
}
