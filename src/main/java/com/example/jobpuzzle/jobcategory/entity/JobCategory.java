package com.example.jobpuzzle.jobcategory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "job_category")
public class JobCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long jobCategoryId;

    private String mainCategory;

    private String subCategory;

    private String careerLevel;

    private LocalDateTime createdAt;

}
