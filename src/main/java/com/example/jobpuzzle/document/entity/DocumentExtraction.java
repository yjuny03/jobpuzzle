package com.example.jobpuzzle.document.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "document_extraction")
public class DocumentExtraction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "extraction_id")
    private Long extractionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private UserDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_extraction_id")
    private DocumentExtraction baseExtraction;

    // 둘 다 null이면 아직 한 번도 확정된 적 없는 검토 중 상태(최초 확정 전 자유 수정 구간)
    @Column(name = "major_version")
    private Integer majorVersion;

    @Column(name = "minor_version")
    private Integer minorVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 20)
    private DocumentExtractionStatus extractionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "version_status", length = 20)
    private DocumentVersionStatus versionStatus;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "ocr_applied", nullable = false)
    private boolean ocrApplied = false;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Builder
    private DocumentExtraction(
            UserDocument document,
            DocumentExtraction baseExtraction,
            Integer majorVersion,
            Integer minorVersion,
            DocumentExtractionStatus extractionStatus,
            DocumentVersionStatus versionStatus,
            String content,
            Integer pageCount,
            boolean ocrApplied,
            String failureReason
    ) {
        this.document = document;
        this.baseExtraction = baseExtraction;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.extractionStatus = extractionStatus;
        this.versionStatus = versionStatus;
        this.content = content;
        this.pageCount = pageCount;
        this.ocrApplied = ocrApplied;
        this.failureReason = failureReason;
    }

    // 이 문서의 첫 확정이면 이 시점에 1.0을 부여하고, 이미 버전이 있으면(수정 저장 시 이미 부여됨) 상태만 바꾼다
    public void confirm() {
        if (this.majorVersion == null) {
            this.majorVersion = 1;
            this.minorVersion = 0;
        }
        this.versionStatus = DocumentVersionStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void supersede() {
        this.versionStatus = DocumentVersionStatus.SUPERSEDED;
    }
}