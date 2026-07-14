package com.example.jobpuzzle.guide.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "job_guide_document")
public class JobGuideDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long guideId;

    private Long jobCategoryId;

    private String title;

    private String sourceType;

    private String filePath;

    private String status;

    private Integer version;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
