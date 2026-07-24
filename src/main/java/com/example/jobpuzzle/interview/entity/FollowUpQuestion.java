package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "follow_up_question")
public class FollowUpQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_up_id")
    private Long followUpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private AnswerEvaluation evaluation;

    @Lob
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_type", nullable = false)
    private FollowUpQuestionType followUpType;

    @Column(name = "target_weakness", length = 100)
    private String targetWeakness;

    @Lob
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Builder
    private FollowUpQuestion(
            AnswerEvaluation evaluation,
            String questionText,
            FollowUpQuestionType followUpType,
            String targetWeakness,
            String reason
    ) {
        this.evaluation = evaluation;
        this.questionText = questionText;
        this.followUpType = followUpType;
        this.targetWeakness = targetWeakness;
        this.reason = reason;
    }
}
