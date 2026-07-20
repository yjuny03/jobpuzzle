package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interview_message")
public class InterviewMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion interviewQuestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false)
    private InterviewMessageSender sender;

    @Lob
    @Column(name = "message_text", columnDefinition = "TEXT", nullable = false)
    private String messageText;

//    Hibernate에게 이 Entity가 DB에 처음 저장될 때 현재 시간을 자동으로 입력을 명하는 어노테이션
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false,  updatable = false) // JPA에게 이 컬럼은 INSERT 때만 저장하고, 나중에 UPDATE할 때는 변경하지 말것을 명하는 옵션
//    private LocalDateTime createdAt;

    @Builder
    private InterviewMessage(
            InterviewQuestion question,
            InterviewMessageSender sender,
            String messageText
    ){
        this.interviewQuestion = question;
        this.sender = sender;
        this.messageText = messageText;
    }

    // 사용자가 입력한 답변 메시지 생성
    public static InterviewMessage createUserMessage(
            InterviewQuestion question,
            String messageText
    ) {
        return InterviewMessage.builder()
                .question(question)
                .sender(InterviewMessageSender.USER)
                .messageText(messageText)
                .build();
    }

    // AI가 생성한 꼬리질문 메시지 생성
    public static InterviewMessage createAiMessage(
            InterviewQuestion question,
            String messageText
    ) {
        return InterviewMessage.builder()
                .question(question)
                .sender(InterviewMessageSender.AI)
                .messageText(messageText)
                .build();
    }

}
