package com.example.jobpuzzle.analysis.rag.entity;

import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 고정 retrieval 결과 안에서 rank와 score만 보관하고 원문은 material chunk를 참조한다. */
@Getter
@Entity
@NoArgsConstructor
@Table(name = "requirement_retrieval_chunk", uniqueConstraints = {
        @UniqueConstraint(name = "uk_requirement_retrieval_chunk_result_rank", columnNames = {"retrieval_result_id", "rank"})
})
public class RequirementRetrievalChunk extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "retrieval_chunk_id")
    private Long retrievalChunkId;

    // 어느 requirement retrieval의 순위 결과인지 연결한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retrieval_result_id", nullable = false)
    private RequirementRetrievalResult retrievalResult;

    // 원문·위치 중복 저장 없이 실제 확정 자료 청크를 참조한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_chunk_id", nullable = false)
    private AnalysisMaterialChunk materialChunk;

    // 검색 결과 내 1부터 시작하는 고정 순위를 보관한다.
    @Column(name = "rank", nullable = false)
    private int rank;

    // 검색 adapter가 계산한 similarity score를 변경 없이 보관한다.
    @Column(name = "similarity_score", nullable = false)
    private double similarityScore;

    // 검증된 검색 순위와 score로 retrieval 결과 청크를 생성한다.
    public static RequirementRetrievalChunk create(RequirementRetrievalResult retrievalResult,
                                                   AnalysisMaterialChunk materialChunk, int rank, double similarityScore) {
        RequirementRetrievalChunk chunk = new RequirementRetrievalChunk();
        chunk.retrievalResult = retrievalResult;
        chunk.materialChunk = materialChunk;
        chunk.rank = rank;
        chunk.similarityScore = similarityScore;
        return chunk;
    }
}
