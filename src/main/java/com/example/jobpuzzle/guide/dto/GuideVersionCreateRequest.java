package com.example.jobpuzzle.guide.dto;

import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 기존 가이드의 적용 범위와 코드는 유지하면서 새 버전의 콘텐츠만 입력받는다.
 * 버전 계보를 임의로 갈라놓지 않기 위해 guideCode·scope 필드는 요청에서 받지 않는다.
 */
@Getter
@NoArgsConstructor
public class GuideVersionCreateRequest {
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
