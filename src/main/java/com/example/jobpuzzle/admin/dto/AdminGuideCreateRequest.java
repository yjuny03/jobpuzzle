package com.example.jobpuzzle.admin.dto;

import com.example.jobpuzzle.guide.entity.GuideScopeType;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AdminGuideCreateRequest {

    @NotBlank(message = "가이드 코드를 입력해주세요.")
    @Size(max = 50, message = "가이드 코드는 50자 이내로 입력해주세요.")
    private String guideCode;

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.")
    private String title;

    @NotNull(message = "적용 범위를 선택해주세요.")
    private GuideScopeType scopeType;

    // scopeType=CATEGORY일 때만 사용
    private Long jobCategoryId;

    // scopeType=PARENT_CATEGORY일 때만 사용
    private String scopeMainCategory;

    @NotBlank(message = "적용 범위 설명을 입력해주세요.")
    @Size(max = 500, message = "적용 범위 설명은 500자 이내로 입력해주세요.")
    private String applicableScope;

    private List<String> evaluationFocus;
    private List<String> evidenceRules;
    private List<String> questionDirection;
    private List<String> avoidQuestions;

    @NotNull(message = "자료 유형을 선택해주세요.")
    private JobGuideDocumentSourceType sourceType;
}