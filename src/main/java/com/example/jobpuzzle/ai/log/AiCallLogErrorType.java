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
    COST_LIMIT
}
