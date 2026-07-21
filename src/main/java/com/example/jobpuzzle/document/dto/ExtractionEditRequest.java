package com.example.jobpuzzle.document.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExtractionEditRequest {

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;
}