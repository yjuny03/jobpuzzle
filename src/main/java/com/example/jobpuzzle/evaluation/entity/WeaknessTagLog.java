package com.example.jobpuzzle.evaluation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "weakness_tag_log")
public class WeaknessTagLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tagLogId;

    private Long userId;

    private Long sessionId;

    private Long evaluationId;

    private String tag;

    private LocalDateTime createdAt;

}
