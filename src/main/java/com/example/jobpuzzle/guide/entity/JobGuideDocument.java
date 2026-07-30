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
import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    // 기존 운영 데이터에 컬럼을 추가할 때 DDL 자동 갱신이 실패하지 않도록 legacy 행은 null을 허용한다.
    // 애플리케이션을 통해 새로 생성되는 가이드는 생성자에서 항상 NOT_STARTED를 저장한다.
    @Column(name = "preprocessing_status", length = 30)
    private GuidePreprocessingStatus preprocessingStatus;

    @Column(name = "preprocessing_model", length = 100)
    private String preprocessingModel;

    @Column(name = "preprocessed_at")
    private LocalDateTime preprocessedAt;

    @Column(name = "preprocessing_error", length = 500)
    private String preprocessingError;

    @Enumerated(EnumType.STRING)
    @Column(name = "indexing_status", length = 30)
    private GuideIndexingStatus indexingStatus;

    @Column(name = "embedding_provider", length = 50)
    private String embeddingProvider;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @Column(name = "indexing_error", length = 500)
    private String indexingError;

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
        this.preprocessingStatus = GuidePreprocessingStatus.NOT_STARTED;
        this.indexingStatus = GuideIndexingStatus.NOT_INDEXED;
        validateScope();
    }

    // 초안 상태의 가이드 제목과 파일 경로를 수정
    public void updateDraft(String title, String filePath) {
        this.title = title;
        this.filePath = filePath;
    }

    /** 관리자 전처리와 검수가 끝나기 전까지 변경 가능한 초안인지 반환한다. */
    public boolean isDraft() {
        return this.status == JobGuideDocumentStatus.DRAFT;
    }

    /** 외부 호출 직전에 전처리 중 상태로 바꾸고 이전 실패 정보는 지운다. */
    public void startPreprocessing() {
        this.preprocessingStatus = GuidePreprocessingStatus.PROCESSING;
        this.preprocessingError = null;
    }

    public boolean isPreprocessingInProgress() {
        return this.preprocessingStatus == GuidePreprocessingStatus.PROCESSING;
    }

    public boolean isReadyForReview() {
        return this.preprocessingStatus == GuidePreprocessingStatus.READY_FOR_REVIEW;
    }

    /** 관리자가 청크를 직접 검수·교체한 경우에도 활성화 가능한 검수 준비 상태로 표시한다. */
    public void markManuallyReadyForReview() {
        this.preprocessingStatus = GuidePreprocessingStatus.READY_FOR_REVIEW;
        this.preprocessingModel = null;
        this.preprocessedAt = LocalDateTime.now();
        this.preprocessingError = null;
        resetIndexing();
    }

    /** OpenAI 구조화 결과를 DRAFT에 반영해 관리자 검수 가능한 상태로 만든다. */
    public void completePreprocessing(
            String model,
            String applicableScope,
            List<String> evaluationFocus,
            List<String> evidenceRules,
            List<String> questionDirection,
            List<String> avoidQuestions
    ) {
        this.applicableScope = applicableScope;
        this.evaluationFocus = List.copyOf(evaluationFocus);
        this.evidenceRules = List.copyOf(evidenceRules);
        this.questionDirection = List.copyOf(questionDirection);
        this.avoidQuestions = List.copyOf(avoidQuestions);
        this.preprocessingStatus = GuidePreprocessingStatus.READY_FOR_REVIEW;
        this.preprocessingModel = model;
        this.preprocessedAt = LocalDateTime.now();
        this.preprocessingError = null;
        resetIndexing();
    }

    /** 원문이나 Provider 응답은 남기지 않고 관리자에게 필요한 안전한 실패 사유만 저장한다. */
    public void failPreprocessing(String safeMessage) {
        this.preprocessingStatus = GuidePreprocessingStatus.FAILED;
        this.preprocessingError = safeMessage;
    }

    /** 외부 벡터 저장 호출 전 중복 인덱싱을 막는다. */
    public void startIndexing() {
        this.indexingStatus = GuideIndexingStatus.INDEXING;
        this.indexingError = null;
    }

    public boolean isIndexingInProgress() {
        return this.indexingStatus == GuideIndexingStatus.INDEXING;
    }

    public boolean isIndexed() {
        return this.indexingStatus == GuideIndexingStatus.INDEXED;
    }

    /** 실제 저장소에 반영된 임베딩 계약을 활성화 전 검증할 수 있도록 고정한다. */
    public void completeIndexing(String provider, String model, int dimension) {
        this.indexingStatus = GuideIndexingStatus.INDEXED;
        this.embeddingProvider = provider;
        this.embeddingModel = model;
        this.embeddingDimension = dimension;
        this.indexedAt = LocalDateTime.now();
        this.indexingError = null;
    }

    public void failIndexing(String safeMessage) {
        this.indexingStatus = GuideIndexingStatus.FAILED;
        this.indexingError = safeMessage;
    }

    private void resetIndexing() {
        this.indexingStatus = GuideIndexingStatus.NOT_INDEXED;
        this.embeddingProvider = null;
        this.embeddingModel = null;
        this.embeddingDimension = null;
        this.indexedAt = null;
        this.indexingError = null;
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
