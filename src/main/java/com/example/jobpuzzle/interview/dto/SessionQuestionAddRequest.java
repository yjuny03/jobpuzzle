package com.example.jobpuzzle.interview.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SessionQuestionAddRequest {

    @NotEmpty
    private List<Long> questionIds;
}
