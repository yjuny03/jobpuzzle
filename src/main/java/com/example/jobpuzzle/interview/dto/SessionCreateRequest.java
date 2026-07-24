package com.example.jobpuzzle.interview.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SessionCreateRequest {

    @NotNull
    private Long questionSetId;

    @NotEmpty
    private List<Long> selectedQuestionIds;
}
