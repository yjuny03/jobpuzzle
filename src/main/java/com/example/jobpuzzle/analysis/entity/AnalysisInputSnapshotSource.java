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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AnalysisCaseSource를 확정 시점에 그대로 고정 복사한 것
@Getter
@Entity
@NoArgsConstructor
@Table(name = "analysis_input_snapshot_source")
public class AnalysisInputSnapshotSource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_source_id")
    private Long snapshotSourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private AnalysisInputSnapshot snapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_id", nullable = false)
    private DocumentExtraction extraction;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private UserDocumentType documentType;

    @Builder
    private AnalysisInputSnapshotSource(AnalysisInputSnapshot snapshot, DocumentExtraction extraction, UserDocumentType documentType) {
        this.snapshot = snapshot;
        this.extraction = extraction;
        this.documentType = documentType;
    }
}