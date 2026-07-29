package com.example.jobpuzzle.guide.repository;

import com.example.jobpuzzle.guide.entity.JobGuideChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobGuideChunkRepository extends JpaRepository<JobGuideChunk, Long> {
    List<JobGuideChunk> findByGuide_GuideIdOrderByChunkIndexAsc(Long guideId);

    long countByGuide_GuideId(Long guideId);

    void deleteByGuide_GuideId(Long guideId);
}
