package com.example.jobpuzzle.jobcategory.dto;

import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.Getter;

// 회원가입 화면의 직무 선택 드롭다운(대분류/중분류/경력) 구성용
@Getter
public class JobCategoryResponse {

    private final Long jobCategoryId;
    private final String mainCategory;
    private final String subCategory;
    private final JobCategoryCareerLevel careerLevel;

    private JobCategoryResponse(Long jobCategoryId, String mainCategory, String subCategory,
                                 JobCategoryCareerLevel careerLevel) {
        this.jobCategoryId = jobCategoryId;
        this.mainCategory = mainCategory;
        this.subCategory = subCategory;
        this.careerLevel = careerLevel;
    }

    public static JobCategoryResponse from(JobCategory jobCategory) {
        return new JobCategoryResponse(
                jobCategory.getJobCategoryId(),
                jobCategory.getMainCategory(),
                jobCategory.getSubCategory(),
                jobCategory.getCareerLevel()
        );
    }
}