package com.example.jobpuzzle.global.security;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 자동로그인(리멤버미) 토큰 저장용 엔티티 - 스프링 시큐리티 PersistentTokenRepository가 사용하는 시리즈/토큰 쌍을 그대로 저장
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    private String series;

    // 로그인 아이디 (User.loginId)
    private String username;

    private String token;

    private LocalDateTime lastUsed;
}
