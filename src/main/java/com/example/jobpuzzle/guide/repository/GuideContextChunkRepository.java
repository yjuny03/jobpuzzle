package com.example.jobpuzzle.guide.repository;

import com.example.jobpuzzle.guide.entity.GuideContextChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideContextChunkRepository extends JpaRepository<GuideContextChunk, Long> {
    List<GuideContextChunk> findByGuideContextResult_GuideContextResultIdOrderByDisplayOrderAsc(Long guideContextResultId);

    void deleteByGuideContextResult_User_UserId(Long userId);
}
