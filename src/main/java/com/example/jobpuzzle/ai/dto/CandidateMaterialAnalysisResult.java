package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// JSON-02: 선택된 지원자 자료를 문서별 근거와 함께 구조화한 AI 응답 계약이다.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateMaterialAnalysisResult {

    private List<UserDocumentType> availableDocumentTypes;
    private Resume resume;
    private CoverLetter coverLetter;
    private Portfolio portfolio;
    private ExperienceNote experienceNote;
    private List<MissingEvidence> missingEvidence;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resume {
        private List<Experience> experiences;
        private List<Skill> skills;
        private List<Role> roles;
        private List<Result> results;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Experience {
        private String experienceId;
        private String title;
        private String period;
        private String summary;
        private List<SourceReference> sourceRefs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skill {
        private String skill;
        private String usageContext;
        private List<SourceReference> sourceRefs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Role {
        private String role;
        private String context;
        private List<SourceReference> sourceRefs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private String result;
        private List<SourceReference> sourceRefs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverLetter {
        private SummaryEvidence motivation;
        private SummaryEvidence values;
        private List<SummaryEvidence> experienceNarratives;
        private SummaryEvidence jobConnection;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryEvidence {
        private String summary;
        private List<SourceReference> sourceRefs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Portfolio {
        private List<Project> projects;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Project {
        private String projectId;
        private String projectName;
        private String structure;
        private String role;
        private List<String> contributions;
        private List<String> techUsageReasons;
        private List<String> problemSolving;
        private List<String> outputs;
        private List<SourceReference> sourceRefs;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceNote {
        private List<StarCandidate> starCandidates;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StarCandidate {
        private String candidateId;
        private String situation;
        private String task;
        private String action;
        private String result;
        private List<StarMissingPart> missingParts;
        private List<SourceReference> sourceRefs;
    }

    public enum StarMissingPart {
        SITUATION,
        TASK,
        ACTION,
        RESULT
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
