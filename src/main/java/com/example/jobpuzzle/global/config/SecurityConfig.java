package com.example.jobpuzzle.global.config;

import com.example.jobpuzzle.global.security.CustomOAuth2UserService;
import com.example.jobpuzzle.global.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Configuration // 이 클래스가 스프링 설정 클래스임을 선언
@EnableWebSecurity // 스프링 시큐리티의 웹 보안 기능을 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    // 카카오 로그인 성공 시 회원 조회/생성을 처리하는 서비스
    private final CustomOAuth2UserService customOAuth2UserService;

    // 자동로그인 시 쿠키의 회원을 다시 찾아오는 데 사용
    private final CustomUserDetailsService customUserDetailsService;

    // 자동로그인 토큰을 DB(RefreshToken 테이블)에 저장/조회하는 구현체
    private final PersistentTokenRepository persistentTokenRepository;

    // 자동로그인 토큰 서명에 쓰는 애플리케이션 키 (외부에 노출되면 안 됨)
    private static final String REMEMBER_ME_KEY = "jobpuzzle-remember-me-key";

    // 자동로그인 유지 기간 - 30일
    private static final int REMEMBER_ME_VALID_SECONDS = 60 * 60 * 24 * 30;

    // 정적 리소스 경로 - 인증 없이 접근 허용할 CSS/JS/이미지 경로
    private static final String[] STATIC_URLS = {
        "/css/**",
        "/js/**",
        "/images/**"
    };

    // 로그인 없이 볼 수 있는 화면(뷰) 경로
    private static final String[] PUBLIC_VIEW_URLS = {
            "/",
            "/index.html",
            "/login",
            "/join",
            "/find-id",
            "/passwd-reset",
            "/unlock",
            "/oauth2/**"
    };

    // 인증 없이 호출 가능한 공개 API 경로
    private static final String[] PUBLIC_API_URLS = {
            "/api/user/join",
            "/api/user/join/**",
            "/api/user/login",
            "/api/user/check-id",
            "/api/user/check-email",
            "/api/user/find-id/**",
            "/api/user/passwd-reset/**",
            "/api/user/unlock/**",
            "/api/job-category"
    };

    // 비밀번호 암호화에 쓰는 인코더
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 아이디/비밀번호 로그인 시 인증을 처리하는 매니저
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 자동로그인(remember-me) 토큰 발급/검증 서비스 - UserService가 로그인/로그아웃 시 직접 호출함
    // (폼로그인을 스프링 기본 필터가 아니라 UserService에서 직접 처리하고 있어서, 자동 연동 대신 빈으로 등록해서 수동 호출)
    // 반환 타입을 인터페이스(RememberMeServices)가 아니라 구현체로 둔 이유:
    // logout()은 RememberMeServices가 아니라 이 클래스가 같이 구현하는 LogoutHandler 인터페이스에 있어서,
    // 인터페이스 타입으로 받으면 UserService에서 logout()을 호출할 수 없음
    @Bean
    public PersistentTokenBasedRememberMeServices rememberMeServices() {
        PersistentTokenBasedRememberMeServices services = new PersistentTokenBasedRememberMeServices(
                REMEMBER_ME_KEY, customUserDetailsService, persistentTokenRepository);
        services.setTokenValiditySeconds(REMEMBER_ME_VALID_SECONDS);
        return services;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(STATIC_URLS).permitAll()
                        .requestMatchers(PUBLIC_VIEW_URLS).permitAll()
                        .requestMatchers(PUBLIC_API_URLS).permitAll()

                        // 위에서 허용 안 한 나머지 요청은 전부 인증(로그인) 필요
                        .anyRequest().authenticated()
                )
                // 세션이 없을 때 자동로그인 쿠키로 재인증을 시도하는 필터 등록
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeServices(rememberMeServices())
                )
                // 카카오 로그인 설정 - 로그인 성공 시 customOAuth2UserService가 회원 조회/생성 처리
                .oauth2Login(oauth2 -> oauth2
                        // 커스텀 로그인 페이지 지정 - 안 하면 스프링이 /login을 가로채서 자체 기본 로그인 화면을 띄워버림
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                );
        return http.build();

    }
  }
