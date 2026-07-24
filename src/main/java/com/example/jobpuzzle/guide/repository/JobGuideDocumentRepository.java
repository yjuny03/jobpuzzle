package com.example.jobpuzzle.guide.repository;

import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentStatus;
import com.example.jobpuzzle.guide.entity.GuideScopeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobGuideDocumentRepository extends JpaRepository<JobGuideDocument, Long> {
    List<JobGuideDocument> findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(
            GuideScopeType scopeType, String mainCategory, String subCategory,
            com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel careerLevel, JobGuideDocumentStatus status);

    List<JobGuideDocument> findByScopeTypeAndScopeMainCategoryAndStatus(
            GuideScopeType scopeType, String scopeMainCategory, JobGuideDocumentStatus status);

    List<JobGuideDocument> findByScopeTypeAndStatus(GuideScopeType scopeType, JobGuideDocumentStatus status);
}
