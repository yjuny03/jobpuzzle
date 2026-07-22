package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.ConfirmedAnalysisSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfirmedAnalysisSnapshotRepository extends JpaRepository<ConfirmedAnalysisSnapshot, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ConfirmedAnalysisSnapshot s " +
            "WHERE s.confirmedBy.userId = :userId " +
            "OR s.candidateAnalysis.user.userId = :userId " +
            "OR s.jobPostingAnalysis.jobPosting.user.userId = :userId")
    void deleteAllRelatedToUser(@Param("userId") Long userId);
}
