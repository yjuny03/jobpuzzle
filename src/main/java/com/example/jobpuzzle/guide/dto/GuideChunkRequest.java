package com.example.jobpuzzle.guide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 관리자 검수 또는 이후 OpenAI 전처리 결과가 저장할 단일 가이드 청크 계약이다. */
@Getter
@NoArgsConstructor
public class GuideChunkRequest {
    @PositiveOrZero
    private int chunkIndex;
    private String title;
    @NotBlank
    private String content;
    private String contentSummary;
}
