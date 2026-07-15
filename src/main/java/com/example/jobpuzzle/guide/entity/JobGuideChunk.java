package com.example.jobpuzzle.guide.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "job_guide_chunk")
public class JobGuideChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chunkId;

    private Long guideId;

    private Integer chunkIndex;

    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    private String contentSummary;

    private String embeddingRef;

    private LocalDateTime createdAt;

}
