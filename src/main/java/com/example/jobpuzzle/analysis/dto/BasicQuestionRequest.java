package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BasicQuestionRequest {
    @NotNull
    private Long jobCategoryId;
    @NotNull
    private JobCategoryCareerLevel careerLevel;
}
