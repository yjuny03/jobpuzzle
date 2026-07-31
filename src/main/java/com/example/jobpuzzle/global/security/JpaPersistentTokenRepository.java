package com.example.jobpuzzle.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

// 스프링 시큐리티 자동로그인(remember-me) 기능이 쓰는 PersistentTokenRepository를
// RefreshToken 테이블(JPA) 기반으로 구현한 어댑터
@Component
@RequiredArgsConstructor
public class JpaPersistentTokenRepository implements PersistentTokenRepository {

    private final RefreshTokenRepository refreshTokenRepository;

    // 자동로그인 최초 로그인 시 토큰 신규 발급
    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        RefreshToken entity = new RefreshToken(
                token.getSeries(),
                token.getUsername(),
                token.getTokenValue(),
                toLocalDateTime(token.getDate())
        );
        refreshTokenRepository.save(entity);
    }

    // 재방문 시 토큰이 갱신될 때(토큰 탈취 방지를 위해 매번 값이 바뀜) 반영
    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        refreshTokenRepository.findById(series).ifPresent(entity -> {
            entity.setToken(tokenValue);
            entity.setLastUsed(toLocalDateTime(lastUsed));
            refreshTokenRepository.save(entity);
        });
    }

    // 쿠키에 담긴 series 값으로 저장된 토큰 조회
    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        return refreshTokenRepository.findById(seriesId)
                .map(entity -> new PersistentRememberMeToken(
                        entity.getUsername(), entity.getSeries(), entity.getToken(), toDate(entity.getLastUsed())
                ))
                .orElse(null);
    }

    // 로그아웃 시 해당 회원의 자동로그인 토큰 전체 삭제
    @Override
    @Transactional
    public void removeUserTokens(String username) {
        refreshTokenRepository.deleteByUsername(username);
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
