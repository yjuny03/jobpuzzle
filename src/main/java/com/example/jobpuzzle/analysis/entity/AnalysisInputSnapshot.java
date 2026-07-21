package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

// AnalysisCase 확정 시점에 생성되는 불변 스냅샷
// 본문 텍스트는 저장하지 않고 확정 추출본을 ID로만 참조
@Getter
@Entity
@NoArgsConstructor
@Table(name = "analysis_input_snapshot")
public class AnalysisInputSnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long snapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_case_id", nullable = false, unique = true)
    private AnalysisCase analysisCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id", nullable = false)
    private JobCategory jobCategory;

    @Builder
    private AnalysisInputSnapshot(AnalysisCase analysisCase, User user, JobCategory jobCategory) {
        this.analysisCase = analysisCase;
        this.user = user;
        this.jobCategory = jobCategory;
    }
}