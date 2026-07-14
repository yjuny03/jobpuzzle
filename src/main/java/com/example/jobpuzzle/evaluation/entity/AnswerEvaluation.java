package com.example.jobpuzzle.evaluation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "answer_evaluation")
public class AnswerEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long evaluationId;

    private Long questionId;

    private Double score;

    private String scoreLabel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String evaluationDetail;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String weaknessTags;

    private String summary;

    private Long aiCallLogId;

    private LocalDateTime createdAt;

}
