package com.example.jobpuzzle.questiontemplate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "question_template")
public class QuestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;

    private Long jobCategoryId;

    private String questionType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String questionText;

    private Integer difficulty;

    private Integer displayOrder;

    private Boolean isActive;

    private LocalDateTime createdAt;

}
