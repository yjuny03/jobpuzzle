package com.example.jobpuzzle.jobposting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "job_posting")
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long jobPostingId;

    private Long userId;

    private Long companyId;

    private Long documentId;

    private Long companyInfoDocumentId;

    private Long jobCategoryId;

    private String jobTitle;

    private String careerLevel;

    private String postingStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
