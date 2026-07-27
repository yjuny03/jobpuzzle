package com.example.jobpuzzle.ai.log;

// Provider 완료와 서버 후처리 실패를 구분하는 안전한 내부 분류다. 공개 API ErrorCode에는 직접 노출하지 않는다.
public enum AiFailureKind {
    NONE,
    OUTPUT_LIMIT_EXCEEDED,
    REFUSAL,
    PROVIDER_COMPLETION_FAILED,
    JSON_OBJECT_INCOMPLETE,
    DTO_PARSE_FAILED,
    SOURCE_REFERENCE_INVALID
}
