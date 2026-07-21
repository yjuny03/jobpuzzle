package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.global.common.BaseEntity;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 분석 작업 단위. DRAFT 상태에서 자료 선택과 직무·경력 기준을 자유롭게 편집하다가
// 분석 입력을 확정하면(JSON-03) INPUT_CONFIRMED로 전환되고 이후 자료 구성을 바꿀 수 없다.
@Getter
@Entity
@NoArgsConstructor
@Table(name = "analysis_case")
public class AnalysisCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_case_id")
    private Long analysisCaseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id", nullable = false)
    private JobCategory jobCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisCaseStatus status;

    @Builder
    private AnalysisCase(User user, JobCategory jobCategory) {
        this.user = user;
        this.jobCategory = jobCategory;
        this.status = AnalysisCaseStatus.DRAFT;
    }

    public boolean isDraft() {
        return this.status == AnalysisCaseStatus.DRAFT;
    }

    public void changeJobCategory(JobCategory jobCategory) {
        this.jobCategory = jobCategory;
    }
}