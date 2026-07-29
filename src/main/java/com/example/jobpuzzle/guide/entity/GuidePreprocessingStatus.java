package com.example.jobpuzzle.guide.entity;

/** 관리자 가이드가 OpenAI 구조화와 검수 준비 중 어느 단계인지 나타낸다. */
public enum GuidePreprocessingStatus {
    NOT_STARTED,
    PROCESSING,
    READY_FOR_REVIEW,
    FAILED
}
