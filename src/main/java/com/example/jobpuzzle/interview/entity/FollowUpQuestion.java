package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 답변 평가 1건당 최대 1개 생성되는 꼬리질문의 메타데이터.
// 원 질문 전체 기준 depth 2까지 연쇄 생성 가능
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "follow_up_question")
public class FollowUpQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_up_id")
    private Long followUpId;

    // 꼬리질문을 발생시킨 평가. 평가당 최대 1개
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "evaluation_id",
            nullable = false,
            unique = true
    )
    private AnswerEvaluation evaluation;

    // 실제 FOLLOW_UP_QUESTION 메시지
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "question_message_id",
            nullable = false,
            unique = true
    )
    private InterviewMessage questionMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_type", nullable = false, length = 30)
    private FollowUpQuestionType followUpType;

    // COMPANY_FIT에서 겨냥한 약점 태그.
    // BASIC 또는 일반 확인 질문은 null
    @Column(name = "target_weakness", length = 100)
    private String targetWeakness;

    @Lob
    @Column(
            name = "reason",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String reason;

    public static FollowUpQuestion create(
            AnswerEvaluation evaluation,
            InterviewMessage questionMessage,
            FollowUpQuestionType followUpType,
            String targetWeakness,
            String reason
    ) {
        FollowUpQuestion followUpQuestion =
                new FollowUpQuestion();

        followUpQuestion.evaluation = evaluation;
        followUpQuestion.questionMessage = questionMessage;
        followUpQuestion.followUpType = followUpType;
        followUpQuestion.targetWeakness = targetWeakness;
        followUpQuestion.reason = reason;

        return followUpQuestion;
    }
}