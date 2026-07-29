package com.example.jobpuzzle.guide.dto;

import com.example.jobpuzzle.guide.entity.GuideScopeType;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GuideRegisterRequest {
    @NotBlank
    private String guideCode;
    @NotNull
    private GuideScopeType scopeType;
    private Long jobCategoryId;
    private String scopeMainCategory;
    @NotBlank
    private String title;
    @NotNull
    private JobGuideDocumentSourceType sourceType;
    private String filePath;
    @NotBlank
    private String version;
    @NotBlank
    private String applicableScope;
    private List<String> evaluationFocus;
    private List<String> evidenceRules;
    private List<String> questionDirection;
    private List<String> avoidQuestions;
}
