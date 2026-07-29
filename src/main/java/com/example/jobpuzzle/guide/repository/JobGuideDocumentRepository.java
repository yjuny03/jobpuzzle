package com.example.jobpuzzle.guide.repository;

import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentStatus;
import com.example.jobpuzzle.guide.entity.GuideScopeType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobGuideDocumentRepository extends JpaRepository<JobGuideDocument, Long> {
    List<JobGuideDocument> findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(
            GuideScopeType scopeType, String mainCategory, String subCategory,
            com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel careerLevel, JobGuideDocumentStatus status);

    List<JobGuideDocument> findByScopeTypeAndScopeMainCategoryAndStatus(
            GuideScopeType scopeType, String scopeMainCategory, JobGuideDocumentStatus status);

    List<JobGuideDocument> findByScopeTypeAndStatus(GuideScopeType scopeType, JobGuideDocumentStatus status);

    boolean existsByGuideCode(String guideCode);

    boolean existsByGuideCodeAndVersion(String guideCode, String version);

    boolean existsByPreviousGuide_GuideId(Long guideId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select guide from JobGuideDocument guide where guide.guideId = :guideId")
    Optional<JobGuideDocument> findWithLockByGuideId(@Param("guideId") Long guideId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<JobGuideDocument> findByScopeTypeAndJobCategory_JobCategoryIdAndStatus(
            GuideScopeType scopeType, Long jobCategoryId, JobGuideDocumentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<JobGuideDocument> findByScopeTypeAndScopeMainCategoryAndStatusOrderByGuideIdAsc(
            GuideScopeType scopeType, String scopeMainCategory, JobGuideDocumentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<JobGuideDocument> findByScopeTypeAndStatusOrderByGuideIdAsc(
            GuideScopeType scopeType, JobGuideDocumentStatus status);
}
