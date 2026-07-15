package com.example.jobpuzzle.interview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "interview_question")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    private Long sessionId;

    private Long snapshotId;

    private InterviewQuestionType questionType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String questionText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String intent;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String evaluationFocus;

    private String relatedRequirement;

    private InterviewQuestionReviewStatus reviewStatus;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    private Integer displayOrder;

    private Long aiCallLogId;

    private Long originQuestionId;

}
