package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;

/**
 * JSON-11에 전달할 기본 모드 입력. DB Entity를 AI 계층에 직접 노출하지 않는다.
 */
public record BasicQuestionGenerationInput(
        Long jobCategoryId,
        String mainCategory,
        String subCategory,
        JobCategoryCareerLevel careerLevel
) {
}
