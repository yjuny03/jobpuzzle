package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "candidate_material_analysis_source")
public class CandidateMaterialAnalysisSource extends BaseEntity {

    /*
     * 모든 JPA 엔티티는 각 행을 구분할 기본키가 필요하다.
     * BaseEntity에는 생성일·수정일만 존재하므로
     * Source 엔티티의 기본키를 별도로 선언한다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "source_id")
    private Long sourceId;

    /*
     * 어떤 사용자 자료 분석 결과에 사용된 문서인지 연결한다.
     * 하나의 CandidateMaterialAnalysis에는 여러 Source가 연결될 수 있다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private CandidateMaterialAnalysis analysis;

    // 실제 분석에 사용한 원본 사용자 문서
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private UserDocument document;

    /*
     * 해당 문서에서 실제 AI 입력으로 사용된 확정 추출 결과.
     * 원본 문서가 나중에 다시 추출되거나 수정되더라도
     * 당시 분석에 사용한 버전을 확인하기 위해 연결한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_id", nullable = false)
    private DocumentExtraction extraction;

    @Builder
    private CandidateMaterialAnalysisSource(
            CandidateMaterialAnalysis analysis,
            UserDocument document,
            DocumentExtraction extraction
    ) {
        this.analysis = analysis;
        this.document = document;
        this.extraction = extraction;
    }
}