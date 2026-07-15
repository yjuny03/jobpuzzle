package com.example.jobpuzzle.user.service;

import com.example.jobpuzzle.user.dto.JoinRequest;
import com.example.jobpuzzle.user.dto.LoginRequest;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    // 비밀번호 암호화/검증에 쓰는 인코더 (SecurityConfig에 등록된 빈)
    private final PasswordEncoder passwordEncoder;

    // 아이디/비밀번호가 맞는지 실제로 검증해주는 스프링 시큐리티 컴포넌트
    private final AuthenticationManager authenticationManager;

    // 아이디/비밀번호 회원가입
    public void join(JoinRequest request) {
        // 아이디 중복 체크
        if (checkIdDuplicate(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        // 이메일 중복 체크
        if (checkEmailDuplicate(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호는 평문 그대로 저장하면 안 되므로 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.createLocalUser(request.getLoginId(), encodedPassword, request.getEmail(), request.getName());
        userRepository.save(user);
    }

    // 아이디 중복 확인 - true면 이미 사용 중인 아이디 (회원가입 화면에서 실시간 체크용)
    public boolean checkIdDuplicate(String loginId) {
        return userRepository.existsByLoginId(loginId);
    }

    // 이메일 중복 확인 - true면 이미 사용 중인 이메일
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }

    public void validatePassword() {
        // TODO: 클래스 정의서 기준으로 구현 (비밀번호 형식 검증 등)
    }

    // 아이디/비밀번호 로그인
    public void login(LoginRequest request, HttpServletRequest httpRequest) {
        // 회원 존재 여부 확인
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));

        // 로그인 실패 누적으로 잠긴 계정이면 비밀번호 확인도 안 하고 바로 차단
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new IllegalStateException("로그인 실패 횟수 초과로 잠긴 계정입니다.");
        }

        try {
            // 실제 아이디/비밀번호 일치 여부 검증
            // 내부적으로 CustomUserDetailsService가 회원을 찾고, PasswordEncoder로 비밀번호를 비교함
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLoginId(), request.getPassword())
            );

            // 로그인 성공했으니 실패 횟수 초기화
            user.resetLoginFailCount();

            // 로그인 성공 정보를 세션에 저장해서 이후 요청에서도 로그인 상태가 유지되게 함
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            httpRequest.getSession(true)
                    .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        } catch (AuthenticationException e) {
            // 비밀번호가 틀린 경우 - 실패 횟수를 올리고(일정 횟수 넘으면 자동 잠금) 에러를 던짐
            user.increaseLoginFailCount();
            throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    public void loginWithOAuth() {
        // 카카오 로그인은 여기가 아니라 CustomOAuth2UserService에서 처리됨
    }

    public void logout() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void findLoginId() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void resetPassword() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void getMyInfo() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void updateMyInfo() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void withdraw() {
        // TODO: 클래스 정의서 기준으로 구현
    }
}
