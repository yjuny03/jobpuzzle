package com.example.jobpuzzle.user.repository;

import com.example.jobpuzzle.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 내 정보 수정 시 이메일 중복 확인용 - 본인 이메일은 중복으로 치지 않음
    boolean existsByEmailAndUserIdNot(String email, Long userId);

    // 내 정보 수정 시 아이디 중복 확인용 - 본인 아이디는 중복으로 치지 않음
    boolean existsByLoginIdAndUserIdNot(String loginId, Long userId);

    // 이메일로 로그인 아이디 찾기
    @Query("SELECT u.loginId FROM User u WHERE u.email = :email")
    String findLoginIdByEmail(@Param("email") String email);

    // 비밀번호 재설정 시 아이디와 이메일이 같은 계정 소유인지 확인
    boolean existsByLoginIdAndEmail(String loginId, String email);
}
