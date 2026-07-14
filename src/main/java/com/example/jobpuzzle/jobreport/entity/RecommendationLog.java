package com.example.jobpuzzle.jobreport.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "recommendation_log")
public class RecommendationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendationId;

    private Long userId;

    private Long criteriaJobCategoryId;

    private Long jobPostingId;

    private String recommendationType;

    private Double similarityScore;

    private Boolean consentGiven;

    private LocalDateTime createdAt;

}
