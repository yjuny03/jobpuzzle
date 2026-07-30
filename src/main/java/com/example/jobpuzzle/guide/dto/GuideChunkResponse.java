package com.example.jobpuzzle.guide.dto;

import com.example.jobpuzzle.guide.entity.JobGuideChunk;

/** 관리자 검수 화면에 노출하는 청크 데이터이며 임베딩 참조값은 외부로 공개하지 않는다. */
public record GuideChunkResponse(
        Long chunkId,
        int chunkIndex,
        String title,
        String content,
        String contentSummary
) {
    public static GuideChunkResponse from(JobGuideChunk chunk) {
        return new GuideChunkResponse(
                chunk.getChunkId(),
                chunk.getChunkIndex(),
                chunk.getTitle(),
                chunk.getContent(),
                chunk.getContentSummary()
        );
    }
}
