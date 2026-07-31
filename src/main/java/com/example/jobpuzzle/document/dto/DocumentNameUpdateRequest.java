package com.example.jobpuzzle.document.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DocumentNameUpdateRequest {

    @NotBlank(message = "자료명을 입력해주세요.")
    private String displayName;
}