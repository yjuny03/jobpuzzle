package com.example.jobpuzzle.guide.client;

import com.example.jobpuzzle.guide.dto.GuidePreprocessingResult;

/** 외부 AI Provider를 가이드 구조화라는 좁은 역할로 제한하는 포트다. */
public interface GuideStructuringClient {
    GuidePreprocessingResult structure(String sourceText);
    String model();
}
