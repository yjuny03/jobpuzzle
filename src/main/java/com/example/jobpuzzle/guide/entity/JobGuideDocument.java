package com.example.jobpuzzle.guide.entity;

import com.example.jobpuzzle.global.common.BaseEntity;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.user.entity.User;
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
@Table(name = "job_guide_document", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_guide_document_code_version", columnNames = {"guide_code", "version"})
})
public class JobGuideDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_id")
    private Long guideId;

    @Column(name = "guide_code", nullable = false, length = 50)
    private String guideCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_guide_id")
    private JobGuideDocument previousGuide;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private GuideScopeType scopeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id")
    private JobCategory jobCategory;

    @Column(name = "scope_main_category", length = 50)
    private String scopeMainCategory;

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

    @Column(name = "applicable_scope", nullable = false, length = 500)
    private String applicableScope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_focus", columnDefinition = "json")
    private List<String> evaluationFocus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_rules", columnDefinition = "json")
    private List<String> evidenceRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_direction", columnDefinition = "json")
    private List<String> questionDirection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avoid_questions", columnDefinition = "json")
    private List<String> avoidQuestions;

    @Builder
    private JobGuideDocument(
            String guideCode,
            JobGuideDocument previousGuide,
            GuideScopeType scopeType,
            JobCategory jobCategory,
            String scopeMainCategory,
            String title,
            JobGuideDocumentSourceType sourceType,
            String filePath,
            String version,
            User createdBy,
            String applicableScope,
            List<String> evaluationFocus,
            List<String> evidenceRules,
            List<String> questionDirection,
            List<String> avoidQuestions
    ){
        this.guideCode = guideCode;
        this.previousGuide = previousGuide;
        this.scopeType = scopeType;
        this.jobCategory = jobCategory;
        this.scopeMainCategory = scopeMainCategory;
        this.title = title;
        this.sourceType = sourceType;
        this.filePath = filePath;
        this.status = JobGuideDocumentStatus.DRAFT;
        this.version = (version == null || version.isBlank())
                ? "v1.0"
                : version;
        this.createdBy = createdBy;
        this.applicableScope = applicableScope;
        this.evaluationFocus = evaluationFocus;
        this.evidenceRules = evidenceRules;
        this.questionDirection = questionDirection;
        this.avoidQuestions = avoidQuestions;
        validateScope();
    }

    // 초안 상태의 가이드 제목과 파일 경로를 수정
    public void updateDraft(String title, String filePath) {
        this.title = title;
        this.filePath = filePath;
    }
    // 가이드를 현재 사용 가능한 활성 상태로 변경
    public void activate() {
        validateScope();
        this.status = JobGuideDocumentStatus.ACTIVE;
    }
    // 기존 가이드를 더 이상 사용하지 않는 비활성 상태로 변경
    public void deactivate() {
        this.status = JobGuideDocumentStatus.INACTIVE;
    }

    // 가이드 범위별 분류 필드 조합을 저장과 활성화 시점에 모두 검증한다.
    private void validateScope() {
        if (scopeType == null) {
            throw new CustomException(ErrorCode.GUIDE_SCOPE_INVALID);
        }
        boolean valid = switch (scopeType) {
            case CATEGORY -> jobCategory != null && scopeMainCategory == null;
            case PARENT_CATEGORY -> jobCategory == null && scopeMainCategory != null && !scopeMainCategory.isBlank();
            case GLOBAL_COMMON -> jobCategory == null && scopeMainCategory == null;
        };
        if (!valid) {
            throw new CustomException(ErrorCode.GUIDE_SCOPE_INVALID);
        }
    }
}
