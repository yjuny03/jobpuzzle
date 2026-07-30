package com.example.jobpuzzle.guide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 관리자가 확인·수정한 단일 가이드 원문 청크 계약이다. */
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
