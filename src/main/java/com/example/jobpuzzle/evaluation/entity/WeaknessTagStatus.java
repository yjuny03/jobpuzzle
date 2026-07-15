package com.example.jobpuzzle.evaluation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "weakness_tag_status")
public class WeaknessTagStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statusId;

    private Long userId;

    private String tag;

    private WeaknessTagResolveStatus status;

    private LocalDateTime firstOccurredAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime lastUpdatedAt;

}
