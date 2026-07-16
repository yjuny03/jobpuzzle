package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.global.common.BaseEntity;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobposting.entity.JobPosting;
import jakarta.persistence.*;
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
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id", nullable = false)
    private JobCategory jobCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "main_tasks",columnDefinition = "json")
    private List<String> mainTasks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements",columnDefinition = "json")
    private List<String> requirements;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred",columnDefinition = "json")
    private List<String> preferred;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "company_values",columnDefinition = "json")
    private List<String> companyValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "core_competencies",columnDefinition = "json")
    private List<String> coreCompetencies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_evidence",columnDefinition = "json")
    private List<String> missingEvidence;

    @Column(name = "is_edited", nullable = false)
    private boolean isEdited = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id")
    private AiCallLog aiCallLog;

    @Builder
    private JobPostingAnalysis(
            JobPosting jobPosting,
            JobCategory jobCategory,
            List<String> mainTasks,
            List<String> requirements,
            List<String> preferred,
            List<String> companyValues,
            List<String> coreCompetencies,
            List<String> missingEvidence,
            AiCallLog aiCallLog
    ) {
        this.jobPosting = jobPosting;
        this.jobCategory = jobCategory;
        this.mainTasks = mainTasks;
        this.requirements = requirements;
        this.preferred = preferred;
        this.companyValues = companyValues;
        this.coreCompetencies = coreCompetencies;
        this.missingEvidence = missingEvidence;
        this.aiCallLog = aiCallLog;
        this.isEdited = false;
    }
}
