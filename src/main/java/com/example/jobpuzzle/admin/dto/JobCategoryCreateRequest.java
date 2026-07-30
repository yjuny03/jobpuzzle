package com.example.jobpuzzle.admin.dto;

import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class JobCategoryCreateRequest {

    @NotBlank(message = "대분류를 입력해주세요.")
    @Size(max = 50, message = "대분류는 50자 이내로 입력해주세요.")
    private String mainCategory;

    @NotBlank(message = "중분류를 입력해주세요.")
    @Size(max = 50, message = "중분류는 50자 이내로 입력해주세요.")
    private String subCategory;

    @NotNull(message = "경력수준을 선택해주세요.")
    private JobCategoryCareerLevel careerLevel;
}