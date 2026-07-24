package com.example.jobpuzzle.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// JSON-01: 채용공고를 우선 기준으로 회사정보를 함께 구조화한 AI 응답 계약이다.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingAnalysisResult {

    private List<Item> mainTasks;
    private List<Requirement> requirements;
    private List<Requirement> preferred;
    private List<Item> companyValues;
    private List<Item> coreCompetencies;
    private List<Conflict> conflicts;
    private List<MissingEvidence> missingEvidence;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String itemId;
        private String text;
        private List<SourceReference> sourceRefs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Requirement {
        private String requirementId;
        private String text;
        private List<SourceReference> sourceRefs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Conflict {
        private String field;
        private String postingValue;
        private String companyInfoValue;
        private String appliedValue;
        private List<SourceReference> sourceRefs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingEvidence {
        private String item;
        private String reason;
    }
}
