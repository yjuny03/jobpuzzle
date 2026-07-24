package com.example.jobpuzzle.interview.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 질문별 채팅형 메시지 기록. 원 질문·원 답변·꼬리질문·꼬리답변을 명시적으로 구분
@Getter
@Entity
@NoArgsConstructor
@Table(name = "interview_message")
public class InterviewMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @Column(nullable = false)
    private Long sessionQuestionId;

    // 꼬리질문·꼬리답변의 기준 메시지
    private Long parentMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewMessageSender sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewMessageType messageType;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String messageText;

    // USER 답변 완료 확정 시각. 질문 메시지는 null
    private LocalDateTime confirmedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private InterviewMessage(
            Long sessionQuestionId,
            Long parentMessageId,
            InterviewMessageSender sender,
            InterviewMessageType messageType,
            String messageText
    ) {
        this.sessionQuestionId = sessionQuestionId;
        this.parentMessageId = parentMessageId;
        this.sender = sender;
        this.messageType = messageType;
        this.messageText = messageText;

        this.createdAt = LocalDateTime.now();
    }

    public void confirm() {
        this.confirmedAt = LocalDateTime.now();
    }
}
