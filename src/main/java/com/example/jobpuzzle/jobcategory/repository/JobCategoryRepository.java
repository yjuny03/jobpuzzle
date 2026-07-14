package com.example.jobpuzzle.jobcategory.repository;

import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {
}
