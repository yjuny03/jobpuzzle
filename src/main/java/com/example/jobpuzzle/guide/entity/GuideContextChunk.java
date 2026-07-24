package com.example.jobpuzzle.guide.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "guide_context_chunk", uniqueConstraints = {
        @UniqueConstraint(name = "uk_guide_context_chunk_result_chunk", columnNames = {"guide_context_result_id", "chunk_id"})
})
public class GuideContextChunk extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_context_chunk_id")
    private Long guideContextChunkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_context_result_id", nullable = false)
    private GuideContextResult guideContextResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", nullable = false)
    private JobGuideChunk jobGuideChunk;

    @Column(name = "similarity_score")
    private Double similarityScore;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public static GuideContextChunk create(GuideContextResult result, JobGuideChunk chunk, int displayOrder) {
        GuideContextChunk contextChunk = new GuideContextChunk();
        contextChunk.guideContextResult = result;
        contextChunk.jobGuideChunk = chunk;
        contextChunk.displayOrder = displayOrder;
        return contextChunk;
    }
}
