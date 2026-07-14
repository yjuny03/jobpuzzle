package com.example.jobpuzzle.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "readiness_result")
public class ReadinessResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long readinessId;

    private Long snapshotId;

    private String status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reason;

    private Long aiCallLogId;

}
