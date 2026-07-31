package com.example.jobpuzzle.analysis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ActionPlanCompletionUpdateRequest {

    @NotNull
    private Boolean completed;
}
