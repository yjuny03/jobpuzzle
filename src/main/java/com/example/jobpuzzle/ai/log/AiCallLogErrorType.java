package com.example.jobpuzzle.ai.log;

public enum AiCallLogErrorType {
    NONE,
    EMPTY_RESPONSE,
    JSON_PARSE_FAIL,
    MISSING_FIELD,
    INVALID_ENUM,
    INVALID_RANGE,
    TIMEOUT,
    PROVIDER_ERROR,
    RATE_LIMIT,
    COST_LIMIT,
    PROMPT_RENDER_FAILED,
    RESPONSE_PARSE_FAILED,
    RESPONSE_VALIDATION_FAILED,
    SOURCE_REFERENCE_INVALID,
    // Provider를 호출하기 전에 stage 입력 계약을 초과한 경우다. 공개 API ErrorCode는 이번 단계에서 추가하지 않는다.
    INPUT_LIMIT_EXCEEDED,
    RESULT_PERSIST_FAILED,
    STALE_RUNNING
}
