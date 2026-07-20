package com.example.jobpuzzle.user.repository;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmailVerificationStore {

    private final Map<String, VerificationCode> store = new ConcurrentHashMap<>();

    // 이메일 인증 코드 저장
    public void save(String email, int code) {
        VerificationCode verificationCode = new VerificationCode(code, LocalDateTime.now().plusMinutes(3));
        store.put(email, verificationCode);
    }

    // 이메일 인증 코드 검증
    public boolean verify(String email, int inputCode) {
        VerificationCode verificationCode = store.get(email);
        // 발송 이력이 없는 이메일
        if (verificationCode == null) {
            return false;
        }
        // 만료된 인증 코드
        if (verificationCode.expireAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        boolean match = verificationCode.code() == inputCode;
        if (match) {
            store.remove(email);
        }
        return match;
    }
}
