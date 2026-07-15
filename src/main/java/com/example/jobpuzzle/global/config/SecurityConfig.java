package com.example.jobpuzzle.global.config;

import com.example.jobpuzzle.global.security.CustomOAuth2UserService;
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

import javax.sql.DataSource;

@Configuration // 이 클래스가 스프링 설정 클래스임을 선언
@EnableWebSecurity // 스프링 시큐리티의 웹 보안 기능을 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    // 카카오 로그인 성공 시 회원 조회/생성을 처리하는 서비스
    private final CustomOAuth2UserService customOAuth2UserService;

    // 정적 리소스 경로 - 인증 없이 접근 허용할 CSS/JS/이미지 경로
    private static final String[] STATIC_URLS = {
        "/css/**",
        "/js/**",
        "/images/**"
    };

    // 로그인 없이 볼 수 있는 화면(뷰) 경로
    private static final String[] PUBLIC_VIEW_URLS = {
            "/",
            "/login",
            "/oauth2/**"
    };

    // 인증 없이 호출 가능한 공개 API 경로
    private static final String[] PUBLIC_API_URLS = {
            "/api/user/join",
            "/api/user/login",
            "/api/user/check-id",
            "/api/user/check-email"
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
                // 카카오 로그인 설정 - 로그인 성공 시 customOAuth2UserService가 회원 조회/생성 처리
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                );
        return http.build();

    }
  }
