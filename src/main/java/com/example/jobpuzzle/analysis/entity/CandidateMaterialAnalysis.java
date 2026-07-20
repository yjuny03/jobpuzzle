package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.global.common.BaseEntity;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "candidate_material_analysis")
public class CandidateMaterialAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id", nullable = false)
    private JobCategory jobCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_document_id")
    private UserDocument resumeDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_document_id")
    private UserDocument coverLetterDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_document_id")
    private UserDocument portfolioDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_note_document_id")
    private UserDocument experienceNoteDocument;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resume_analysis",columnDefinition = "json")
    private Resume resumeAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cover_letter_analysis",columnDefinition = "json")
    private CoverLetter coverLetterAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "portfolio_analysis",columnDefinition = "json")
    private Portfolio portfolioAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "experience_note_analysis",columnDefinition = "json")
    private ExperienceNote experienceNoteAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_evidence",columnDefinition = "json")
    private List<String> missingEvidence;

    @Column(name= "is_edited",nullable = false)
    private boolean isEdited = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id")
    private AiCallLog aiCallLog;

    @Builder
    private CandidateMaterialAnalysis(
            User user,
            JobCategory jobCategory,
            UserDocument resumeDocument,
            UserDocument coverLetterDocument,
            UserDocument portfolioDocument,
            UserDocument experienceNoteDocument,
            Resume resumeAnalysis,
            CoverLetter coverLetterAnalysis,
            Portfolio portfolioAnalysis,
            ExperienceNote experienceNoteAnalysis,
            List<String> missingEvidence,
            AiCallLog aiCallLog
    ){
        this.user = user;
        this.jobCategory = jobCategory;
        this.resumeDocument = resumeDocument;
        this.coverLetterDocument = coverLetterDocument;
        this.portfolioDocument = portfolioDocument;
        this.experienceNoteDocument = experienceNoteDocument;
        this.resumeAnalysis = resumeAnalysis;
        this.coverLetterAnalysis = coverLetterAnalysis;
        this.portfolioAnalysis = portfolioAnalysis;
        this.experienceNoteAnalysis = experienceNoteAnalysis;
        this.missingEvidence = missingEvidence;
        this.isEdited = false;
        this.aiCallLog = aiCallLog;
    }

    /*
     * AI 응답 DTO의 중첩 객체를 엔티티에서 그대로 사용하면,
     * AI 응답 구조 변경 시 기존 DB JSON 저장 구조에도 직접 영향을 줄 수 있다.
     * 따라서 AI 응답 DTO와 DB 저장 모델을 분리하고, from()을 통해 저장용 객체로 변환한다.
     */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resume{
        private List<Experience> experiences;
        private List<String> skills;
        private List<String> roles;
        private List<String> results;

        public static Resume from(
                CandidateMaterialAnalysisResult.Resume result
        ) {
            // 입력값이 null 일 경우 NullPointerException 발생
            if (result == null) {
                return null;
            }
            // AI 응답의 하위 객체 목록을 DB 저장용 하위 객체 목록으로 변환
            return Resume.builder()
                    .experiences(
                            result.getExperiences() == null
                                    ? List.of()
                                    : result.getExperiences().stream()
                                    .map(Experience::from)
                                    .toList()
                    )
                    .skills(result.getSkills())
                    .roles(result.getRoles())
                    .results(result.getResults())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Experience{
        private String title;
        private String period;

        public static Experience from(
                CandidateMaterialAnalysisResult.Experience result
        ) {
            if (result == null) {
                return null;
            }
            return Experience.builder()
                    .title(result.getTitle())
                    .period(result.getPeriod())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CoverLetter{
        private String motivation;
        private String values;
        private List<String> experienceNarratives;
        private String jobConnection;

        public static CoverLetter from(CandidateMaterialAnalysisResult.CoverLetter result){
            if (result == null) {
                return null;
            }

            return CoverLetter.builder()
                    .motivation(result.getMotivation())
                    .values(result.getValues())
                    .experienceNarratives(result.getExperienceNarratives())
                    .jobConnection(result.getJobConnection())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Portfolio{
        private List<ProjectStructure> projectStructure;
        private List<String> contributions;
        private List<String> techUsageReason;
        private List<String> outputs;

        // AI 응답의 하위 객체 목록을 DB 저장용 하위 객체 목록으로 변환
        public static Portfolio from (CandidateMaterialAnalysisResult.Portfolio result){
            if (result == null) {
                return null;
            }

            return Portfolio.builder()
                    .projectStructure(
                            result.getProjectStructure() == null
                            ? List.of()
                            : result.getProjectStructure().stream()
                                    .map(ProjectStructure::from)
                                    .toList())
                    .contributions(result.getContributions())
                    .techUsageReason(result.getTechUsageReason())
                    .outputs(result.getOutputs())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProjectStructure{
        private String projectName;
        private String role;

        public static ProjectStructure from(CandidateMaterialAnalysisResult.ProjectStructure result){
            if (result == null) {
                return null;
            }

            return ProjectStructure.builder()
                    .projectName(result.getProjectName())
                    .role(result.getRole())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExperienceNote{
        private List<StarCandidate> starCandidates;

        // AI 응답의 하위 객체 목록을 DB 저장용 하위 객체 목록으로 변환
        public static ExperienceNote from(CandidateMaterialAnalysisResult.ExperienceNote result){
            if (result == null) {
                return null;
            }

            return ExperienceNote.builder()
                    .starCandidates(
                            result.getStarCandidates() == null
                            ? List.of()
                                    : result.getStarCandidates().stream()
                                    .map(StarCandidate::from)
                                    .toList()
                    )
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StarCandidate {
        private String situation;
        private String task;

        public static StarCandidate from(CandidateMaterialAnalysisResult.StarCandidate result){
            if (result == null) {
                return null;
            }

            return StarCandidate.builder()
                    .situation(result.getSituation())
                    .task(result.getTask())
                    .build();
        }
    }

}
