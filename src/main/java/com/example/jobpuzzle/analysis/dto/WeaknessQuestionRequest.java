package com.example.jobpuzzle.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WeaknessQuestionRequest {
    @NotBlank
    private String targetWeaknessTag;
}
