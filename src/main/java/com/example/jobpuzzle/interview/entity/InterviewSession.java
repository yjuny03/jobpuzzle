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

    private InterviewSessionMode mode;

    private Long guideId;

    private String guideVersion;

    private String promptVersion;

    private InterviewSessionStatus status;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime deletedAt;

    private String targetWeaknessTag;

}
