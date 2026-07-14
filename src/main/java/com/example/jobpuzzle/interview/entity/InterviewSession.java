package com.example.jobpuzzle.interview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "interview_session")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionId;

    private Long userId;

    private Long snapshotId;

    private String mode;

    private Long guideId;

    private Integer guideVersion;

    private Integer promptVersion;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime deletedAt;

    private String targetWeaknessTag;

}
