package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.global.common.BaseEntity;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
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

    // 분석 결과의 소유 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 사용자 자료 분석에 적용한 직무 분류
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id", nullable = false)
    private JobCategory jobCategory;

    /*
     * 기존에는 이력서·자기소개서·포트폴리오·경험노트 문서를
     * 각각 단일 FK로 보관했기 때문에 자료 유형별 문서 1개만 연결할 수 있었다.
     *
     * 사용자가 업로드한 여러 자료를 모두 분석할 수 있도록
     * 분석 결과와 원본 문서의 관계를 CandidateMaterialAnalysisSource로 분리한다.
     *
     * 한 분석 결과에 여러 Source 행이 연결되며,
     * 각 Source에는 실제 분석에 사용한 UserDocument와 확정 추출본을 저장한다.
     */
    @OneToMany(
            mappedBy = "analysis",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CandidateMaterialAnalysisSource> sources = new ArrayList<>();

    // 여러 이력서 내용을 종합하여 생성한 AI 분석 결과
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resume_analysis", columnDefinition = "json")
    private Resume resumeAnalysis;

    // 여러 자기소개서 내용을 종합하여 생성한 AI 분석 결과
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cover_letter_analysis", columnDefinition = "json")
    private CoverLetter coverLetterAnalysis;

    // 여러 포트폴리오 내용을 종합하여 생성한 AI 분석 결과
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "portfolio_analysis", columnDefinition = "json")
    private Portfolio portfolioAnalysis;

    // 여러 경험노트 내용을 종합하여 생성한 AI 분석 결과
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "experience_note_analysis", columnDefinition = "json")
    private ExperienceNote experienceNoteAnalysis;

    // 사용자 자료에서 확인되지 않아 보완이 필요한 근거 목록
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_evidence", columnDefinition = "json")
    private List<String> missingEvidence;

    // 사용자가 AI 분석 결과를 직접 수정했는지 여부
    @Column(name = "is_edited", nullable = false)
    private boolean isEdited = false;

    // 해당 분석 결과를 생성한 AI 호출 기록
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id")
    private AiCallLog aiCallLog;

    @Builder
    private CandidateMaterialAnalysis(
            User user,
            JobCategory jobCategory,
            Resume resumeAnalysis,
            CoverLetter coverLetterAnalysis,
            Portfolio portfolioAnalysis,
            ExperienceNote experienceNoteAnalysis,
            List<String> missingEvidence,
            AiCallLog aiCallLog
    ) {
        this.user = user;
        this.jobCategory = jobCategory;
        this.resumeAnalysis = resumeAnalysis;
        this.coverLetterAnalysis = coverLetterAnalysis;
        this.portfolioAnalysis = portfolioAnalysis;
        this.experienceNoteAnalysis = experienceNoteAnalysis;
        this.missingEvidence = missingEvidence;
        this.isEdited = false;
        this.aiCallLog = aiCallLog;
    }

    /*
     * 분석에 사용한 원본 자료 연결 정보를 추가한다.
     *
     * CandidateMaterialAnalysisSource를 생성할 때
     * source.analysis에는 현재 CandidateMaterialAnalysis 객체가 설정되어 있어야 한다.
     *
     * 예:
     * CandidateMaterialAnalysisSource source =
     *         CandidateMaterialAnalysisSource.builder()
     *                 .analysis(analysis)
     *                 .document(document)
     *                 .extraction(extraction)
     *                 .documentType(document.getDocumentType())
     *                 .build();
     *
     * analysis.addSource(source);
     */
    public void addSource(CandidateMaterialAnalysisSource source) {
        if (source == null) {
            return;
        }

        this.sources.add(source);
    }

    /*
     * AI 응답 DTO의 중첩 객체를 엔티티에서 그대로 사용하면,
     * AI 응답 구조 변경 시 기존 DB JSON 저장 구조에도 직접 영향을 줄 수 있다.
     * 따라서 AI 응답 DTO와 DB 저장 모델을 분리하고,
     * from()을 통해 AI 응답 객체를 DB 저장용 객체로 변환한다.
     */

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resume {

        private List<Experience> experiences;
        private List<String> skills;
        private List<String> roles;
        private List<String> results;

        public static Resume from(
                CandidateMaterialAnalysisResult.Resume result
        ) {
            // AI 응답에 이력서 분석 결과가 없으면 null로 저장
            if (result == null) {
                return null;
            }

            // AI 응답의 경력 목록을 DB 저장용 경력 목록으로 변환
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
    public static class Experience {

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
    public static class CoverLetter {

        private String motivation;
        private String values;
        private List<String> experienceNarratives;
        private String jobConnection;

        public static CoverLetter from(
                CandidateMaterialAnalysisResult.CoverLetter result
        ) {
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
    public static class Portfolio {

        private List<ProjectStructure> projectStructure;
        private List<String> contributions;
        private List<String> techUsageReason;
        private List<String> outputs;

        public static Portfolio from(
                CandidateMaterialAnalysisResult.Portfolio result
        ) {
            if (result == null) {
                return null;
            }

            // AI 응답의 프로젝트 목록을 DB 저장용 프로젝트 목록으로 변환
            return Portfolio.builder()
                    .projectStructure(
                            result.getProjectStructure() == null
                                    ? List.of()
                                    : result.getProjectStructure().stream()
                                    .map(ProjectStructure::from)
                                    .toList()
                    )
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
    public static class ProjectStructure {

        private String projectName;
        private String role;

        public static ProjectStructure from(
                CandidateMaterialAnalysisResult.ProjectStructure result
        ) {
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
    public static class ExperienceNote {

        private List<StarCandidate> starCandidates;

        public static ExperienceNote from(
                CandidateMaterialAnalysisResult.ExperienceNote result
        ) {
            if (result == null) {
                return null;
            }

            // AI 응답의 STAR 후보 목록을 DB 저장용 STAR 후보 목록으로 변환
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

        public static StarCandidate from(
                CandidateMaterialAnalysisResult.StarCandidate result
        ) {
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