package com.example.jobpuzzle.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // 본인 파트별로 주석 다시고 안에 추가하세요. 중복되는 건 찾아보시고 재사용합시다.

    // Common
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_001", "요청한 리소스를 찾을 수 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_002", "요청 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_003", "서버 내부 오류가 발생했습니다."),

    //AI
    AI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "AI_001", "AI 응답이 유효하지 않습니다."),

    //analysis
    SNAPSHOT_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_001", "확정된 분석 스냅샷을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message){
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
