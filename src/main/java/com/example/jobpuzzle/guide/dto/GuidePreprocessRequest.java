package com.example.jobpuzzle.guide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파일 추출기와 OpenAI 호출을 분리하기 위한 가이드 원문 계약.
 * PDF·DOCX도 텍스트 추출이 끝난 뒤 이 API에 동일한 형태로 전달한다.
 */
@Getter
@NoArgsConstructor
public class GuidePreprocessRequest {
    @NotBlank
    @Size(max = 100_000)
    private String sourceText;
}
