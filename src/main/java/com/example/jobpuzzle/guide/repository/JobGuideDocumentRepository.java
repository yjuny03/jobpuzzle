package com.example.jobpuzzle.guide.repository;

import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobGuideDocumentRepository extends JpaRepository<JobGuideDocument, Long> {
}
