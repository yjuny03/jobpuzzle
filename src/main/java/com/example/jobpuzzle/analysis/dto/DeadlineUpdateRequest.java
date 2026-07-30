package com.example.jobpuzzle.analysis.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class DeadlineUpdateRequest {
    private LocalDate deadline;
}
