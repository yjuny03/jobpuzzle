package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AnalysisCase에 연결된 CONFIRMED DocumentExtraction 1건. documentType은 추가 시점의
// 문서 유형을 그대로 복사해두어 유형별 개수 검증(공고 1건/사용자 자료 1건 이상)을 조인 없이 처리한다.
@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "analysis_case_source",
        uniqueConstraints = @UniqueConstraint(columnNames = {"analysis_case_id", "extraction_id"})
)
public class AnalysisCaseSource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_case_source_id")
    private Long analysisCaseSourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_case_id", nullable = false)
    private AnalysisCase analysisCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_id", nullable = false)
    private DocumentExtraction extraction;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private UserDocumentType documentType;

    @Builder
    private AnalysisCaseSource(AnalysisCase analysisCase, DocumentExtraction extraction, UserDocumentType documentType) {
        this.analysisCase = analysisCase;
        this.extraction = extraction;
        this.documentType = documentType;
    }
}