package com.example.jobpuzzle.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "action_plan")
public class ActionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planId;

    private Long snapshotId;

    private Long matchId;

    private String relatedRequirement;

    private ActionPlanMatchLevel matchLevel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String missingPoint;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String suggestion;

    private LocalDateTime deadline;

    private ActionPlanStatus status;

    private LocalDateTime completedAt;

    private Long aiCallLogId;

    private LocalDateTime createdAt;

}
