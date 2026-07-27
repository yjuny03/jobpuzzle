package com.example.jobpuzzle.ai.log;

// PROVIDER_CALL만 외부 AI 요청·비용 집계 대상이다. AGGREGATE_RESULT는 분할 결과의 호환용 대표 log다.
public enum AiCallLogRole { PROVIDER_CALL, PREPARATION_FAILURE, AGGREGATE_RESULT }
