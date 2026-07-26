package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 확정 분석 스냅샷의 원문 위치를 보존하는 결정적 청크다. */
@Getter
@Entity
@NoArgsConstructor
@Table(name = "analysis_material_chunk", uniqueConstraints = {
        @UniqueConstraint(name = "uk_analysis_material_chunk_source_version_index",
                columnNames = {"snapshot_source_id", "chunking_version", "chunk_index"})
})
public class AnalysisMaterialChunk extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long chunkId;

    // 청크가 속한 분석 입력을 고정해 snapshot 단위 격리 조회에 사용한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private AnalysisInputSnapshot snapshot;

    // snapshot 안의 정확한 선택 자료를 고정해 재생성·재사용 단위로 사용한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_source_id", nullable = false)
    private AnalysisInputSnapshotSource snapshotSource;

    // 원문 추출본을 직접 연결해 위치와 본문을 검증할 수 있게 한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_id", nullable = false)
    private DocumentExtraction extraction;

    // 소유자 ID를 보관해 user + snapshot 격리 조회를 단순하고 명시적으로 만든다.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 원문 문서 ID는 추출본과 별도로 검색 결과의 출처를 빠르게 식별한다.
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    // 문서 유형은 유형별 검색 필터와 표시를 위해 snapshot 시점 값으로 보관한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private UserDocumentType documentType;

    // 시작 페이지는 청크가 가리키는 원문 페이지 범위를 표시한다.
    @Column(name = "page_start", nullable = false)
    private int pageStart;

    // 종료 페이지는 현재 MVP의 단일 페이지 청크 범위를 명시한다.
    @Column(name = "page_end", nullable = false)
    private int pageEnd;

    // 시작 위치는 DocumentExtraction.content 기준의 inclusive 문자 인덱스다.
    @Column(name = "char_start", nullable = false)
    private int charStart;

    // 종료 위치는 DocumentExtraction.content 기준의 end-exclusive 문자 인덱스다.
    @Column(name = "char_end", nullable = false)
    private int charEnd;

    // source별 고정 순서는 재호출 시 동일한 결과와 검증 기준을 제공한다.
    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    // 저장 본문은 반드시 extraction content의 지정 범위 substring과 동일하다.
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    // 본문과 분할 규칙 버전의 결정적 해시는 중복 여부와 재현성을 확인한다.
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    // 분할 규칙 변경 시 기존 청크와 새 청크를 분리하는 버전 값이다.
    @Column(name = "chunking_version", nullable = false, length = 50)
    private String chunkingVersion;

    // 검증된 원문 범위로만 청크를 만들어 저장 본문과 offset의 불일치를 막는다.
    public static AnalysisMaterialChunk create(
            AnalysisInputSnapshot snapshot,
            AnalysisInputSnapshotSource snapshotSource,
            DocumentExtraction extraction,
            Long userId,
            Long documentId,
            UserDocumentType documentType,
            int pageStart,
            int pageEnd,
            int charStart,
            int charEnd,
            int chunkIndex,
            String content,
            String contentHash,
            String chunkingVersion
    ) {
        AnalysisMaterialChunk chunk = new AnalysisMaterialChunk();
        chunk.snapshot = snapshot;
        chunk.snapshotSource = snapshotSource;
        chunk.extraction = extraction;
        chunk.userId = userId;
        chunk.documentId = documentId;
        chunk.documentType = documentType;
        chunk.pageStart = pageStart;
        chunk.pageEnd = pageEnd;
        chunk.charStart = charStart;
        chunk.charEnd = charEnd;
        chunk.chunkIndex = chunkIndex;
        chunk.content = content;
        chunk.contentHash = contentHash;
        chunk.chunkingVersion = chunkingVersion;
        return chunk;
    }
}
