package com.example.jobpuzzle.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // json 정의서에 없는 키 값이 들어와도 에러 안남
public class JobPostingAnalysisResult {

    private List<String> mainTasks;
    private List<String> requirements;
    private List<String> preferred;
    private List<String> companyValues;
    private List<String> coreCompetencies;
    private List<String> missingEvidence;

}
