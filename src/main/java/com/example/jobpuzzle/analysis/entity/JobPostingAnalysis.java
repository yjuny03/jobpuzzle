package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.log.AiCallLog;
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

@Getter
@Entity
@NoArgsConstructor
@Table(name = "job_posting_analysis")
public class JobPostingAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    // JSON-01 결과는 분석 입력 스냅샷 하나당 정확히 한 건만 저장한다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false, unique = true)
    private AnalysisInputSnapshot snapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "main_tasks", columnDefinition = "json")
    private List<Item> mainTasks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements", columnDefinition = "json")
    private List<Requirement> requirements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred", columnDefinition = "json")
    private List<Requirement> preferred;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "company_values", columnDefinition = "json")
    private List<Item> companyValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "core_competencies", columnDefinition = "json")
    private List<Item> coreCompetencies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conflicts", columnDefinition = "json")
    private List<Conflict> conflicts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_evidence", columnDefinition = "json")
    private List<MissingEvidence> missingEvidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id", nullable = false)
    private AiCallLog aiCallLog;

    @Builder
    private JobPostingAnalysis(
            AnalysisInputSnapshot snapshot,
            List<Item> mainTasks,
            List<Requirement> requirements,
            List<Requirement> preferred,
            List<Item> companyValues,
            List<Item> coreCompetencies,
            List<Conflict> conflicts,
            List<MissingEvidence> missingEvidence,
            AiCallLog aiCallLog
    ) {
        this.snapshot = snapshot;
        this.mainTasks = mainTasks;
        this.requirements = requirements;
        this.preferred = preferred;
        this.companyValues = companyValues;
        this.coreCompetencies = coreCompetencies;
        this.conflicts = conflicts;
        this.missingEvidence = missingEvidence;
        this.aiCallLog = aiCallLog;
    }

    // AI 응답 계약을 DB 저장 값으로 명시적으로 변환한다.
    public static JobPostingAnalysis from(
            AnalysisInputSnapshot snapshot,
            JobPostingAnalysisResult result,
            AiCallLog aiCallLog
    ) {
        return JobPostingAnalysis.builder()
                .snapshot(snapshot)
                .mainTasks(map(result.getMainTasks(), Item::from))
                .requirements(map(result.getRequirements(), Requirement::from))
                .preferred(map(result.getPreferred(), Requirement::from))
                .companyValues(map(result.getCompanyValues(), Item::from))
                .coreCompetencies(map(result.getCoreCompetencies(), Item::from))
                .conflicts(map(result.getConflicts(), Conflict::from))
                .missingEvidence(map(result.getMissingEvidence(), MissingEvidence::from))
                .aiCallLog(aiCallLog)
                .build();
    }

    private static <S, T> List<T> map(List<S> values, java.util.function.Function<S, T> mapper) {
        return values == null ? List.of() : values.stream().map(mapper).toList();
    }

    private static List<AnalysisSourceReference> sourceRefs(
            List<com.example.jobpuzzle.ai.dto.SourceReference> values
    ) {
        return map(values, AnalysisSourceReference::from);
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String itemId;
        private String text;
        private List<AnalysisSourceReference> sourceRefs;

        public static Item from(JobPostingAnalysisResult.Item item) {
            return Item.builder()
                    .itemId(item.getItemId())
                    .text(item.getText())
                    .sourceRefs(JobPostingAnalysis.sourceRefs(item.getSourceRefs()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Requirement {
        private String requirementId;
        private String text;
        private List<AnalysisSourceReference> sourceRefs;

        public static Requirement from(JobPostingAnalysisResult.Requirement requirement) {
            return Requirement.builder()
                    .requirementId(requirement.getRequirementId())
                    .text(requirement.getText())
                    .sourceRefs(JobPostingAnalysis.sourceRefs(requirement.getSourceRefs()))
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Conflict {
        private String field;
        private String postingValue;
        private String companyInfoValue;
        private String appliedValue;
        private List<AnalysisSourceReference> sourceRefs;

        public static Conflict from(JobPostingAnalysisResult.Conflict conflict) {
            return Conflict.builder()
                    .field(conflict.getField())
                    .postingValue(conflict.getPostingValue())
                    .companyInfoValue(conflict.getCompanyInfoValue())
                    .appliedValue(conflict.getAppliedValue())
                    .sourceRefs(JobPostingAnalysis.sourceRefs(conflict.getSourceRefs()))
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

        public static MissingEvidence from(JobPostingAnalysisResult.MissingEvidence missing) {
            return MissingEvidence.builder()
                    .item(missing.getItem())
                    .reason(missing.getReason())
                    .build();
        }
    }
}
