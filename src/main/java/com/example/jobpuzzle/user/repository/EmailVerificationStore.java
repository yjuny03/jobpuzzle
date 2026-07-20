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

    // 이메일 인증 코드 검증 - 성공해도 삭제하지 않고 유효시간 만료로만 무효화됨 (검증 후 별도 단계에서 재확인이 필요한 흐름 지원용)
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
        return verificationCode.code() == inputCode;
    }

    // 인증 코드를 즉시 무효화 - 인증 코드를 활용한 민감한 작업(비밀번호 변경 등)이 완료된 시점에 호출
    public void invalidate(String email) {
        store.remove(email);
    }
}
