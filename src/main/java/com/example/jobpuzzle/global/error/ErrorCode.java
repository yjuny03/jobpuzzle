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
    SNAPSHOT_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_001", "확정된 분석 스냅샷을 찾을 수 없습니다."),

    //user - 회원가입/로그인
    USER_LOGIN_ID_DUPLICATE(HttpStatus.CONFLICT, "USER_001", "이미 사용 중인 아이디입니다."),
    USER_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 이메일입니다."),
    USER_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USER_003", "아이디 또는 비밀번호가 올바르지 않습니다."),
    USER_ACCOUNT_LOCKED(HttpStatus.LOCKED, "USER_004", "로그인 실패 횟수 초과로 잠긴 계정입니다. \n이메일 인증으로 잠금 해제할 수 있습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_005", "존재하지 않는 회원입니다."),
    USER_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_006", "비밀번호는 영문/숫자를 포함해 8~20자로 입력해주세요."),
    USER_EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_007", "등록되지 않은 이메일입니다."),
    EMAIL_CODE_INCORRECT(HttpStatus.BAD_REQUEST, "USER_008", "올바르지 않은 인증 코드입니다."),
    USER_LOGIN_ID_EMAIL_MISMATCH(HttpStatus.NOT_FOUND, "USER_009", "아이디 또는 이메일을 확인해주세요."),
    USER_ACCOUNT_NOT_LOCKED(HttpStatus.BAD_REQUEST, "USER_010", "잠기지 않은 계정입니다."),

    //document
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENT_001", "파일을 저장하거나 불러오는 중 오류가 발생했습니다."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_002", "존재하지 않는 자료입니다."),
    EXTRACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_003", "존재하지 않는 추출 버전입니다."),
    EXTRACTION_NOT_LATEST_DRAFT(HttpStatus.BAD_REQUEST, "DOCUMENT_004", "최신 DRAFT 버전만 확정할 수 있습니다."),
    EXTRACTION_NOT_CONFIRMABLE(HttpStatus.BAD_REQUEST, "DOCUMENT_005", "확정할 수 있는 DRAFT 버전이 없습니다."),
    EXTRACTION_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "DOCUMENT_006", "확정되지 않은 추출본이 포함되어 있습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "DOCUMENT_007", "지원하지 않는 파일 형식입니다. PDF, JPG, PNG 파일만 업로드할 수 있습니다."),
    INVALID_DIRECT_INPUT_TYPE(HttpStatus.BAD_REQUEST, "DOCUMENT_008", "직접 입력은 채용공고·회사정보·경험정리 유형만 가능합니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "DOCUMENT_009", "파일 크기는 10MB를 초과할 수 없습니다."),

    //job category
    JOB_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,"job_category_001","존재하지 않는 직업 분류 입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message){
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
