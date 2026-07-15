package com.example.jobpuzzle.user.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import com.example.jobpuzzle.user.dto.JoinRequest;
import com.example.jobpuzzle.user.dto.LoginRequest;
import com.example.jobpuzzle.user.dto.MyInfoUpdateRequest;
import com.example.jobpuzzle.user.dto.UserInfoResponse;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    // 비밀번호 암호화/검증에 쓰는 인코더 (SecurityConfig에 등록된 빈)
    private final PasswordEncoder passwordEncoder;

    // 아이디/비밀번호가 맞는지 실제로 검증해주는 스프링 시큐리티 컴포넌트
    private final AuthenticationManager authenticationManager;

    // 자동로그인(리멤버미) 쿠키 발급/취소 - SecurityConfig에 등록된 빈
    // (인터페이스가 아니라 구현체 타입인 이유는 SecurityConfig의 rememberMeServices() 빈 주석 참고 - logout()이 LogoutHandler 쪽에 있어서 필요함)
    private final PersistentTokenBasedRememberMeServices rememberMeServices;

    // 자동로그인 토큰 DB 삭제용 - rememberMeServices.logout()은 쿠키만 지우고 DB 토큰은 안 지워서 별도로 호출
    private final PersistentTokenRepository persistentTokenRepository;

    // 비밀번호 정책 - 영문/숫자 각각 1자 이상 포함, 8~20자
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,20}$");

    // 아이디/비밀번호 회원가입
    public void join(JoinRequest request) {
        // 아이디 중복 체크
        if (checkIdDuplicate(request.getLoginId())) {
            throw new CustomException(ErrorCode.USER_LOGIN_ID_DUPLICATE);
        }
        // 이메일 중복 체크
        if (checkEmailDuplicate(request.getEmail())) {
            throw new CustomException(ErrorCode.USER_EMAIL_DUPLICATE);
        }
        // 비밀번호 정책 검증
        validatePassword(request.getPassword());

        // 비밀번호는 평문 그대로 저장하면 안 되므로 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.createLocalUser(request.getLoginId(), encodedPassword, request.getEmail(),
                request.getName(), request.getDefaultJobCategoryId());
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

    // 비밀번호 길이/조합 정책 검증 - 정책 위반 시 예외
    public void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new CustomException(ErrorCode.USER_INVALID_PASSWORD);
        }
    }

    // 아이디/비밀번호 로그인
    public void login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 회원 존재 여부 확인
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_LOGIN_FAILED));

        // 로그인 실패 누적으로 잠긴 계정이면 비밀번호 확인도 안 하고 바로 차단
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new CustomException(ErrorCode.USER_ACCOUNT_LOCKED);
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

            // 자동로그인 체크한 경우 - 리멤버미 쿠키 + DB 토큰 발급
            if (Boolean.TRUE.equals(request.getAutoLogin())) {
                rememberMeServices.loginSuccess(httpRequest, httpResponse, authentication);
            }

        } catch (AuthenticationException e) {
            // 비밀번호가 틀린 경우 - 실패 횟수를 올리고(일정 횟수 넘으면 자동 잠금) 에러를 던짐
            user.increaseLoginFailCount();
            throw new CustomException(ErrorCode.USER_LOGIN_FAILED);
        }
    }

    public void loginWithOAuth() {
        // 카카오 로그인은 여기가 아니라 CustomOAuth2UserService에서 처리됨
    }

    // 로그아웃 - 세션 무효화 + 시큐리티 컨텍스트 정리 + 자동로그인 쿠키/토큰 정리
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 쿠키 만료 처리 (브라우저에 남은 자동로그인 쿠키 제거)
        rememberMeServices.logout(httpRequest, httpResponse, authentication);
        // DB에 저장된 자동로그인 토큰도 함께 삭제
        if (authentication != null) {
            persistentTokenRepository.removeUserTokens(authentication.getName());
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    public void findLoginId() {
        // TODO: 이메일 인증 인프라 준비되면 구현 (현재 메일 발송 설정 없음)
    }

    public void resetPassword() {
        // TODO: 이메일 인증 인프라 준비되면 구현 (현재 메일 발송 설정 없음)
    }

    // 내 정보 조회 - 로그인된 회원 기준
    public UserInfoResponse getMyInfo(CustomUserDetails userDetails) {
        return UserInfoResponse.from(userDetails.getUser());
    }

    // 내 정보 수정 - 이름/이메일/기본 관심 직무
    // userDetails가 들고 있는 User는 인증 시점에 조회된 엔티티라 현재 트랜잭션에서 영속 상태가 아닐 수 있어서,
    // userId로 다시 조회한 영속 엔티티를 수정해야 변경 감지(dirty checking)로 실제 반영됨
    public void updateMyInfo(MyInfoUpdateRequest request, CustomUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUser().getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 이메일을 바꾸는 경우에만 중복 체크 (본인 이메일은 중복으로 안 침)
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmailAndUserIdNot(request.getEmail(), user.getUserId())) {
            throw new CustomException(ErrorCode.USER_EMAIL_DUPLICATE);
        }

        user.updateProfile(request.getName(), request.getEmail(), request.getDefaultJobCategoryId());
    }

    // 회원 탈퇴 - 상태 변경 후 로그인 상태도 함께 정리 (updateMyInfo와 같은 이유로 다시 조회해서 수정)
    public void withdraw(CustomUserDetails userDetails, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = userRepository.findById(userDetails.getUser().getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.withdraw();
        logout(httpRequest, httpResponse);
    }
}
