package com.example.jobpuzzle.user.repository;

import com.example.jobpuzzle.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 카카오 로그인 시 이미 가입된 소셜 계정인지 확인
    Optional<User> findBySocialProviderAndSocialId(String socialProvider, String socialId);

    // 아이디/비밀번호 로그인 시 아이디로 회원 조회
    Optional<User> findByLoginId(String loginId);

    // 회원가입 시 아이디 중복 확인용
    boolean existsByLoginId(String loginId);

    // 회원가입 시 이메일 중복 확인용
    boolean existsByEmail(String email);
}
