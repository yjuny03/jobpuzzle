package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.InterviewSectionKey;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class InterviewViewPreferenceRequest {
    @NotNull
    private List<InterviewSectionKey> sectionOrder;
}
