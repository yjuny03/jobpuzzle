package com.example.jobpuzzle.jobcategory.repository;

import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {

    Optional<JobCategory> findByMainCategoryAndSubCategoryAndCareerLevel(
            String mainCategory,
            String subCategory,
            JobCategoryCareerLevel careerLevel
    );
}
