package com.example.jobpuzzle.guide.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "job_guide_chunk",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_job_guide_chunk_guide_index",
                        columnNames = {"guide_id", "chunk_index"}
                )
        }
)
public class JobGuideChunk extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long chunkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private JobGuideDocument guide;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "title", length = 200)
    private String title;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "content_summary", length = 500)
    private String contentSummary;

    // 외부 벡터 DB에 저장된 임베딩 데이터의 참조 키
    @Column(name = "embedding_ref", length = 200)
    private String embeddingRef;

    @Builder
    private JobGuideChunk(
            JobGuideDocument guide,
            int chunkIndex,
            String title,
            String content,
            String contentSummary,
            String embeddingRef
    ) {
        this.guide = guide;
        this.chunkIndex = chunkIndex;
        this.title = title;
        this.content = content;
        this.contentSummary = contentSummary;
        this.embeddingRef = embeddingRef;
    }
}