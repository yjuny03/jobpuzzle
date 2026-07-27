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
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_004", "접근 권한이 없습니다."),

    //AI
    AI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "AI_001", "AI 응답이 유효하지 않습니다."),
    AI_PROMPT_TEMPLATE_NOT_FOUND(HttpStatus.CONFLICT, "AI_002", "실행 단계에 사용할 활성 프롬프트 템플릿이 없습니다."),

    //analysis
    JSON05_RESULT_INTEGRITY_CONFLICT(HttpStatus.CONFLICT, "ANALYSIS_010", "JSON-05 result integrity conflict"),
    VECTOR_INDEX_NOT_READY(HttpStatus.CONFLICT, "ANALYSIS_011", "분석 자료의 vector 색인이 준비되지 않았습니다."),
    EMBEDDING_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "ANALYSIS_012", "임베딩 제공자 호출에 실패했습니다."),
    EMBEDDING_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "ANALYSIS_013", "임베딩 제공자 요청 한도를 초과했습니다."),
    VECTOR_STORE_ERROR(HttpStatus.BAD_GATEWAY, "ANALYSIS_014", "vector 저장소 호출에 실패했습니다."),
    VECTOR_COLLECTION_INCOMPATIBLE(HttpStatus.CONFLICT, "ANALYSIS_015", "vector collection의 모델 또는 차원 계약이 일치하지 않습니다."),
    SNAPSHOT_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_001", "확정된 분석 스냅샷을 찾을 수 없습니다."),
    ANALYSIS_CASE_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_002", "존재하지 않는 분석 작업입니다."),
    ANALYSIS_CASE_NOT_DRAFT(HttpStatus.BAD_REQUEST, "ANALYSIS_003", "DRAFT 상태의 분석 작업만 자료·기준을 변경할 수 있습니다."),
    ANALYSIS_CASE_SOURCE_DUPLICATE(HttpStatus.BAD_REQUEST, "ANALYSIS_004", "이미 연결된 자료입니다."),
    ANALYSIS_CASE_JOB_POSTING_ALREADY_SELECTED(HttpStatus.BAD_REQUEST, "ANALYSIS_005", "채용공고는 1건만 선택할 수 있습니다. 기존 자료를 먼저 제거해주세요."),
    ANALYSIS_CASE_SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_006", "연결된 자료를 찾을 수 없습니다."),
    ANALYSIS_CASE_JOB_POSTING_REQUIRED(HttpStatus.BAD_REQUEST, "ANALYSIS_007", "채용공고를 정확히 1건 선택해야 합니다."),
    ANALYSIS_CASE_USER_MATERIAL_REQUIRED(HttpStatus.BAD_REQUEST, "ANALYSIS_008", "이력서·자기소개서·포트폴리오·경험정리 중 최소 1건을 선택해야 합니다."),
    ANALYSIS_CASE_NOT_READY(HttpStatus.BAD_REQUEST, "ANALYSIS_009", "입력 확정 또는 분석 진행 상태의 분석 작업만 실행할 수 있습니다."),

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
    // 문구는 USER_LOGIN_FAILED와 동일하게 유지하되, 로그인 화면 힌트 분기를 위해 코드만 분리
    USER_LOGIN_ID_NOT_FOUND(HttpStatus.UNAUTHORIZED, "USER_011", "아이디 또는 비밀번호가 올바르지 않습니다."),

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
    CHANGE_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "DOCUMENT_010", "이미 확정된 적 있는 자료를 수정할 때는 자잘한 수정/큰 수정 여부를 선택해야 합니다."),
    EXTRACTION_ALREADY_VERSIONED(HttpStatus.BAD_REQUEST, "DOCUMENT_011", "이미 확정 이력이 있는 자료는 재추출할 수 없습니다. 수정 저장을 이용해주세요."),
    MIXED_FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DOCUMENT_012", "PDF와 이미지를 함께 업로드할 수 없습니다."),
    PDF_MULTIPLE_FILES_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "DOCUMENT_013", "PDF는 한 번에 1개 파일만 업로드할 수 있습니다."),
    IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "DOCUMENT_014", "이미지는 최대 20장까지 업로드할 수 있습니다."),

    //job category
    JOB_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,"job_category_001","존재하지 않는 직업 분류 입니다."),

    // guide
    GUIDE_ACTIVE_DUPLICATED(HttpStatus.CONFLICT, "GUIDE_001", "동일 검색 범위에 사용 가능한 가이드가 여러 건 존재합니다."),
    GUIDE_SCOPE_INVALID(HttpStatus.BAD_REQUEST, "GUIDE_002", "가이드 적용 범위와 분류 값 조합이 올바르지 않습니다."),
    GUIDE_ACTIVE_NOT_FOUND(HttpStatus.CONFLICT, "GUIDE_003", "해당 직무에 사용할 활성 분석 가이드가 없습니다."),


    // interview - 면접 세션·질문 선택·답변 진행
    QUESTION_SET_NOT_READY(HttpStatus.CONFLICT, "INTERVIEW_001", "사용할 수 있는 질문 묶음이 준비되지 않았습니다."),
    INVALID_QUESTION_SELECTION(HttpStatus.UNPROCESSABLE_ENTITY, "INTERVIEW_002", "선택한 질문이 질문 묶음과 일치하지 않습니다."),
    INTERVIEW_SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "INTERVIEW_003", "해당 질문 묶음에 진행 중인 면접 세션이 있습니다."),
    INTERVIEW_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW_004", "면접 세션을 찾을 수 없습니다."),
    SESSION_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW_005", "면접 세션 질문을 찾을 수 없습니다."),
    SESSION_NOT_EDITABLE(HttpStatus.CONFLICT, "INTERVIEW_006", "완료 또는 취소된 세션은 변경할 수 없습니다."),
    SESSION_CANNOT_BE_CANCELED(HttpStatus.CONFLICT, "INTERVIEW_007", "답변을 제출한 세션은 취소할 수 없습니다."),
    SESSION_CANNOT_BE_COMPLETED(HttpStatus.CONFLICT, "INTERVIEW_008", "완료 조건을 충족하지 못한 세션입니다."),
    ANSWER_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "INTERVIEW_009", "이미 확정된 답변입니다."),
    FOLLOW_UP_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "INTERVIEW_010", "추가 꼬리질문을 생성할 수 없습니다."),
    WEAKNESS_NOT_AVAILABLE(HttpStatus.CONFLICT, "INTERVIEW_011", "약점 보완에 사용할 미해결 약점이 없습니다."),
    ANSWER_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "INTERVIEW_012", "답변 기준 메시지를 찾을 수 없습니다."),

    // interview / report
    INTERVIEW_SESSION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "REPORT_002", "완료되지 않은 세션은 최종 리포트를 생성할 수 없습니다."),
    FINAL_REPORT_NOT_GENERATABLE(HttpStatus.BAD_REQUEST, "REPORT_003", "평가에 성공한 답변이 없어 최종 리포트를 생성할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message){
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
