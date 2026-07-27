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

    @Column(name = "chunk_title_snapshot", length = 200)
    private String chunkTitleSnapshot;

    @Lob
    @Column(name = "chunk_content_snapshot", columnDefinition = "LONGTEXT")
    private String chunkContentSnapshot;

    @Column(name = "chunk_content_hash_snapshot", length = 64)
    private String chunkContentHashSnapshot;

    public static GuideContextChunk create(GuideContextResult result, JobGuideChunk chunk, int displayOrder) {
        GuideContextChunk contextChunk = new GuideContextChunk();
        contextChunk.guideContextResult = result;
        contextChunk.jobGuideChunk = chunk;
        contextChunk.displayOrder = displayOrder;
        contextChunk.chunkTitleSnapshot = chunk.getTitle();
        contextChunk.chunkContentSnapshot = chunk.getContent();
        contextChunk.chunkContentHashSnapshot = sha256(chunk.getContent());
        return contextChunk;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
