package com.example.jobpuzzle.interview.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 답변 평가 1건당 최대 1개 생성되는 꼬리질문의 메타데이터. 원 질문 전체 기준 depth 2까지 연쇄 생성 가능
@Getter
@Entity
@NoArgsConstructor
@Table(name = "follow_up_question")
public class FollowUpQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followUpId;

    // 꼬리질문을 발생시킨 평가. 평가당 최대 1개(UNIQUE)
    @Column(nullable = false, unique = true)
    private Long evaluationId;

    // 실제 FOLLOW_UP_QUESTION 메시지(UNIQUE)
    @Column(nullable = false, unique = true)
    private Long questionMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FollowUpQuestionType followUpType;

    // COMPANY_FIT에서 겨냥한 약점 태그. BASIC 또는 일반 확인 질문은 null
    private String targetWeakness;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private FollowUpQuestion(
            Long evaluationId,
            Long questionMessageId,
            FollowUpQuestionType followUpType,
            String targetWeakness,
            String reason
    ) {
        this.evaluationId = evaluationId;
        this.questionMessageId = questionMessageId;
        this.followUpType = followUpType;
        this.targetWeakness = targetWeakness;
        this.reason = reason;

        this.createdAt = LocalDateTime.now();
    }
}
