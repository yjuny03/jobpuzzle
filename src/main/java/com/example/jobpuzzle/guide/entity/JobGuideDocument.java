package com.example.jobpuzzle.guide.entity;

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
@Table(name = "job_guide_document")
public class JobGuideDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_id")
    private Long guideId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id",nullable = false)
    private JobCategory jobCategory;

    @Column(name = "title",nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private JobGuideDocumentSourceType sourceType;

    @Column(name = "file_path",length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobGuideDocumentStatus status;

    @Column(name = "version",nullable = false, length = 20)
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Builder
    private JobGuideDocument(
            JobCategory jobCategory,
            String title,
            JobGuideDocumentSourceType sourceType,
            String filePath,
            String version,
            User createdBy
    ){
        this.jobCategory = jobCategory;
        this.title = title;
        this.sourceType = sourceType;
        this.filePath = filePath;
        this.status = JobGuideDocumentStatus.DRAFT;
        this.version = (version == null || version.isBlank())
                ? "v1.0"
                : version;
        this.createdBy = createdBy;
    }

    // 초안 상태의 가이드 제목과 파일 경로를 수정
    public void updateDraft(String title, String filePath) {
        this.title = title;
        this.filePath = filePath;
    }
    // 가이드를 현재 사용 가능한 활성 상태로 변경
    public void activate() {
        this.status = JobGuideDocumentStatus.ACTIVE;
    }
    // 기존 가이드를 더 이상 사용하지 않는 비활성 상태로 변경
    public void deactivate() {
        this.status = JobGuideDocumentStatus.INACTIVE;
    }
}
