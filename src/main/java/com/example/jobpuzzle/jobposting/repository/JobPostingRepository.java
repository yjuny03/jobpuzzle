package com.example.jobpuzzle.jobposting.repository;

import com.example.jobpuzzle.jobposting.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    void deleteByUser_UserId(Long userId);
}
