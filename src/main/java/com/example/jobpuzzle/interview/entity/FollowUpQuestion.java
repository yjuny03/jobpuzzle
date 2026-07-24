package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "follow_up_question")
public class FollowUpQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_up_id")
    private Long followUpId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false, unique = true)
    private AnswerEvaluation evaluation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_message_id", nullable = false, unique = true)
    private InterviewMessage questionMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_type", nullable = false, length = 30)
    private FollowUpQuestionType followUpType;

    @Column(name = "target_weakness", length = 100)
    private String targetWeakness;

    @Lob
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    public static FollowUpQuestion create(
            AnswerEvaluation evaluation,
            InterviewMessage questionMessage,
            FollowUpQuestionType followUpType,
            String targetWeakness,
            String reason
    ) {
        FollowUpQuestion followUp = new FollowUpQuestion();
        followUp.evaluation = evaluation;
        followUp.questionMessage = questionMessage;
        followUp.followUpType = followUpType;
        followUp.targetWeakness = targetWeakness;
        followUp.reason = reason;
        return followUp;
    }
}
