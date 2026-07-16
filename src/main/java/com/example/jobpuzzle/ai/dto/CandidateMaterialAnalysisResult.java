package com.example.jobpuzzle.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateMaterialAnalysisResult {

    private Resume resume;
    private CoverLetter coverLetter;
    private Portfolio portfolio;
    private ExperienceNote experienceNote;
    private List<String> missingEvidence;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Resume{
        private List<Experience> experiences;
        private List<String> skills;
        private List<String> roles;
        private List<String> results;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Experience {
        private String title;
        private String period;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CoverLetter{
        private String motivation;
        private String values;
        private List<String> experienceNarratives;
        private String jobConnection;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Portfolio{
        private List<ProjectStructure> projectStructure;
        private List<String> contributions;
        private List<String> techUsageReason;
        private List<String> outputs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectStructure{
        private String projectName;
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExperienceNote{
        private List<StarCandidate> starCandidates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StarCandidate {
        private String situation;
        private String task;
    }
}
