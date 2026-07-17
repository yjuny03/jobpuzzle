package com.example.jobpuzzle.jobposting.entity;

import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.global.common.BaseEntity;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "job_posting")
public class JobPosting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_posting_id")
    private Long jobPostingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // JOB_POSTING 유형의 공고 원문
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private UserDocument document;

    // COMPANY_INFO 유형의 회사정보 문서
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_info_document_id")
    private UserDocument companyInfoDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id")
    private JobCategory jobCategory;

    @Column(name = "job_title", length = 200)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "career_level", length = 20)
    private JobPostingCareerLevel careerLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_status", nullable = false, length = 20)
    private JobPostingStatus postingStatus;

    @Builder
    private JobPosting(
            User user,
            Company company,
            UserDocument document,
            UserDocument companyInfoDocument,
            JobCategory jobCategory,
            String jobTitle,
            JobPostingCareerLevel careerLevel
    ) {
        this.user = user;
        this.company = company;
        this.document = document;
        this.companyInfoDocument = companyInfoDocument;
        this.jobCategory = jobCategory;
        this.jobTitle = jobTitle;
        this.careerLevel = careerLevel;
        this.postingStatus = JobPostingStatus.NORMAL;
    }

    // 공고의 직무 분류, 직무명, 경력 수준을 수정
    public void updateClassification(
            JobCategory jobCategory,
            String jobTitle,
            JobPostingCareerLevel careerLevel
    ) {
        this.jobCategory = jobCategory;
        this.jobTitle = jobTitle;
        this.careerLevel = careerLevel;
    }

    // 공고 분류 상태를 오분류로 변경
    public void markAsMisclassified() {
        this.postingStatus = JobPostingStatus.MISCLASSIFIED;
    }

    // 공고 분류 상태를 정상으로 복구
    public void restoreNormalStatus() {
        this.postingStatus = JobPostingStatus.NORMAL;
    }
}