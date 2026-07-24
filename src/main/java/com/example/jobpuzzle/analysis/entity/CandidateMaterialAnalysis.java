package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.function.Function;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "candidate_material_analysis")
public class CandidateMaterialAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    // JSON-02 결과는 분석 입력 스냅샷 하나당 정확히 한 건만 저장한다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false, unique = true)
    private AnalysisInputSnapshot snapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "available_document_types", nullable = false, columnDefinition = "json")
    private List<UserDocumentType> availableDocumentTypes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resume_analysis", columnDefinition = "json")
    private Resume resumeAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cover_letter_analysis", columnDefinition = "json")
    private CoverLetter coverLetterAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "portfolio_analysis", columnDefinition = "json")
    private Portfolio portfolioAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "experience_note_analysis", columnDefinition = "json")
    private ExperienceNote experienceNoteAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_evidence", columnDefinition = "json")
    private List<MissingEvidence> missingEvidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id", nullable = false)
    private AiCallLog aiCallLog;

    @Builder
    private CandidateMaterialAnalysis(
            AnalysisInputSnapshot snapshot,
            List<UserDocumentType> availableDocumentTypes,
            Resume resumeAnalysis,
            CoverLetter coverLetterAnalysis,
            Portfolio portfolioAnalysis,
            ExperienceNote experienceNoteAnalysis,
            List<MissingEvidence> missingEvidence,
            AiCallLog aiCallLog
    ) {
        this.snapshot = snapshot;
        this.availableDocumentTypes = availableDocumentTypes;
        this.resumeAnalysis = resumeAnalysis;
        this.coverLetterAnalysis = coverLetterAnalysis;
        this.portfolioAnalysis = portfolioAnalysis;
        this.experienceNoteAnalysis = experienceNoteAnalysis;
        this.missingEvidence = missingEvidence;
        this.aiCallLog = aiCallLog;
    }

    // AI 응답 계약을 DB 저장 값으로 명시적으로 변환한다.
    public static CandidateMaterialAnalysis from(
            AnalysisInputSnapshot snapshot,
            CandidateMaterialAnalysisResult result,
            AiCallLog aiCallLog
    ) {
        return CandidateMaterialAnalysis.builder()
                .snapshot(snapshot)
                .availableDocumentTypes(copy(result.getAvailableDocumentTypes()))
                .resumeAnalysis(Resume.from(result.getResume()))
                .coverLetterAnalysis(CoverLetter.from(result.getCoverLetter()))
                .portfolioAnalysis(Portfolio.from(result.getPortfolio()))
                .experienceNoteAnalysis(ExperienceNote.from(result.getExperienceNote()))
                .missingEvidence(map(result.getMissingEvidence(), MissingEvidence::from))
                .aiCallLog(aiCallLog)
                .build();
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static <S, T> List<T> map(List<S> values, Function<S, T> mapper) {
        return values == null ? List.of() : values.stream().map(mapper).toList();
    }

    private static List<AnalysisSourceReference> sourceRefs(List<SourceReference> values) {
        return map(values, AnalysisSourceReference::from);
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resume {
        private List<Experience> experiences;
        private List<Skill> skills;
        private List<Role> roles;
        private List<Result> results;

        public static Resume from(CandidateMaterialAnalysisResult.Resume resume) {
            if (resume == null) {
                return null;
            }
            return Resume.builder()
                    .experiences(map(resume.getExperiences(), Experience::from))
                    .skills(map(resume.getSkills(), Skill::from))
                    .roles(map(resume.getRoles(), Role::from))
                    .results(map(resume.getResults(), Result::from))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Experience {
        private String experienceId;
        private String title;
        private String period;
        private String summary;
        private List<AnalysisSourceReference> sourceRefs;

        public static Experience from(CandidateMaterialAnalysisResult.Experience experience) {
            return Experience.builder()
                    .experienceId(experience.getExperienceId())
                    .title(experience.getTitle())
                    .period(experience.getPeriod())
                    .summary(experience.getSummary())
                    .sourceRefs(CandidateMaterialAnalysis.sourceRefs(experience.getSourceRefs()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skill {
        private String skill;
        private String usageContext;
        private List<AnalysisSourceReference> sourceRefs;

        public static Skill from(CandidateMaterialAnalysisResult.Skill skill) {
            return Skill.builder()
                    .skill(skill.getSkill())
                    .usageContext(skill.getUsageContext())
                    .sourceRefs(CandidateMaterialAnalysis.sourceRefs(skill.getSourceRefs()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Role {
        private String role;
        private String context;
        private List<AnalysisSourceReference> sourceRefs;

        public static Role from(CandidateMaterialAnalysisResult.Role role) {
            return Role.builder()
                    .role(role.getRole())
                    .context(role.getContext())
                    .sourceRefs(CandidateMaterialAnalysis.sourceRefs(role.getSourceRefs()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private String result;
        private List<AnalysisSourceReference> sourceRefs;

        public static Result from(CandidateMaterialAnalysisResult.Result result) {
            return Result.builder()
                    .result(result.getResult())
                    .sourceRefs(CandidateMaterialAnalysis.sourceRefs(result.getSourceRefs()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverLetter {
        private SummaryEvidence motivation;
        private SummaryEvidence values;
        private List<SummaryEvidence> experienceNarratives;
        private SummaryEvidence jobConnection;

        public static CoverLetter from(CandidateMaterialAnalysisResult.CoverLetter coverLetter) {
            if (coverLetter == null) {
                return null;
            }
            return CoverLetter.builder()
                    .motivation(SummaryEvidence.from(coverLetter.getMotivation()))
                    .values(SummaryEvidence.from(coverLetter.getValues()))
                    .experienceNarratives(map(
                            coverLetter.getExperienceNarratives(),
                            SummaryEvidence::from
                    ))
                    .jobConnection(SummaryEvidence.from(coverLetter.getJobConnection()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryEvidence {
        private String summary;
        private List<AnalysisSourceReference> sourceRefs;

        public static SummaryEvidence from(CandidateMaterialAnalysisResult.SummaryEvidence evidence) {
            if (evidence == null) {
                return null;
            }
            return SummaryEvidence.builder()
                    .summary(evidence.getSummary())
                    .sourceRefs(CandidateMaterialAnalysis.sourceRefs(evidence.getSourceRefs()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Portfolio {
        private List<Project> projects;

        public static Portfolio from(CandidateMaterialAnalysisResult.Portfolio portfolio) {
            if (portfolio == null) {
                return null;
            }
            return Portfolio.builder()
                    .projects(map(portfolio.getProjects(), Project::from))
                    .build();
        }
    }

    @Getter
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
        private List<AnalysisSourceReference> sourceRefs;

        public static Project from(CandidateMaterialAnalysisResult.Project project) {
            return Project.builder()
                    .projectId(project.getProjectId())
                    .projectName(project.getProjectName())
                    .structure(project.getStructure())
                    .role(project.getRole())
                    .contributions(copy(project.getContributions()))
                    .techUsageReasons(copy(project.getTechUsageReasons()))
                    .problemSolving(copy(project.getProblemSolving()))
                    .outputs(copy(project.getOutputs()))
                    .sourceRefs(CandidateMaterialAnalysis.sourceRefs(project.getSourceRefs()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceNote {
        private List<StarCandidate> starCandidates;

        public static ExperienceNote from(CandidateMaterialAnalysisResult.ExperienceNote experienceNote) {
            if (experienceNote == null) {
                return null;
            }
            return ExperienceNote.builder()
                    .starCandidates(map(experienceNote.getStarCandidates(), StarCandidate::from))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StarCandidate {
        private String candidateId;
        private String situation;
        private String task;
        private String action;
        private String result;
        private List<CandidateMaterialAnalysisResult.StarMissingPart> missingParts;
        private List<AnalysisSourceReference> sourceRefs;

        public static StarCandidate from(CandidateMaterialAnalysisResult.StarCandidate candidate) {
            return StarCandidate.builder()
                    .candidateId(candidate.getCandidateId())
                    .situation(candidate.getSituation())
                    .task(candidate.getTask())
                    .action(candidate.getAction())
                    .result(candidate.getResult())
                    .missingParts(copy(candidate.getMissingParts()))
                    .sourceRefs(CandidateMaterialAnalysis.sourceRefs(candidate.getSourceRefs()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingEvidence {
        private String item;
        private String reason;

        public static MissingEvidence from(CandidateMaterialAnalysisResult.MissingEvidence missing) {
            return MissingEvidence.builder()
                    .item(missing.getItem())
                    .reason(missing.getReason())
                    .build();
        }
    }
}
