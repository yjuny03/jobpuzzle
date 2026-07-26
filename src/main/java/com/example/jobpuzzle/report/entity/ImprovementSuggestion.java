package com.example.jobpuzzle.report.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// COMPANY_FIT 최종 리포트의 이력서·자기소개서·포트폴리오·경험정리 보완 제안
@Getter
@Entity
@NoArgsConstructor
@Table(name = "improvement_suggestion")
public class ImprovementSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long suggestionId;

    @Column(nullable = false)
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImprovementSuggestionTargetType targetType;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String suggestionText;

    // 유형 내 표시 순서
    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ImprovementSuggestion(
            Long reportId,
            ImprovementSuggestionTargetType targetType,
            String suggestionText,
            Integer displayOrder
    ) {
        this.reportId = reportId;
        this.targetType = targetType;
        this.suggestionText = suggestionText;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;

        this.createdAt = LocalDateTime.now();
    }
}
