package com.example.jobpuzzle.interview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "follow_up_question")
public class FollowUpQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followUpId;

    private Long evaluationId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String questionText;

    private String followUpType;

    private String targetWeakness;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reason;

    private LocalDateTime createdAt;

}
