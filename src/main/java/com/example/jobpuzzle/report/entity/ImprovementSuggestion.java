package com.example.jobpuzzle.report.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "improvement_suggestion")
public class ImprovementSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long suggestionId;

    private Long reportId;

    private String targetType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String suggestionText;

    private LocalDateTime createdAt;

}
