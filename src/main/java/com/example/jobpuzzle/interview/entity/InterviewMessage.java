package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "session_question_id", nullable = false)
    private InterviewSessionQuestion sessionQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private InterviewMessage parentMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender", nullable = false, length = 20)
    private InterviewMessageSender sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private InterviewMessageType messageType;

    @Lob
    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    public static InterviewMessage originalQuestion(InterviewSessionQuestion sessionQuestion) {
        return create(
                sessionQuestion,
                null,
                InterviewMessageSender.AI,
                InterviewMessageType.ORIGINAL_QUESTION,
                sessionQuestion.getQuestionTextSnapshot(),
                null
        );
    }

    public static InterviewMessage answer(
            InterviewSessionQuestion sessionQuestion,
            InterviewMessage parentQuestion,
            AnswerType answerType,
            String messageText
    ) {
        InterviewMessageType messageType = answerType == AnswerType.ORIGINAL_ANSWER
                ? InterviewMessageType.ORIGINAL_ANSWER
                : InterviewMessageType.FOLLOW_UP_ANSWER;
        return create(
                sessionQuestion,
                parentQuestion,
                InterviewMessageSender.USER,
                messageType,
                messageText,
                LocalDateTime.now()
        );
    }

    public static InterviewMessage followUpQuestion(
            InterviewSessionQuestion sessionQuestion,
            InterviewMessage parentAnswer,
            String messageText
    ) {
        return create(
                sessionQuestion,
                parentAnswer,
                InterviewMessageSender.AI,
                InterviewMessageType.FOLLOW_UP_QUESTION,
                messageText,
                null
        );
    }

    private static InterviewMessage create(
            InterviewSessionQuestion sessionQuestion,
            InterviewMessage parentMessage,
            InterviewMessageSender sender,
            InterviewMessageType messageType,
            String messageText,
            LocalDateTime confirmedAt
    ) {
        InterviewMessage message = new InterviewMessage();
        message.sessionQuestion = sessionQuestion;
        message.parentMessage = parentMessage;
        message.sender = sender;
        message.messageType = messageType;
        message.messageText = messageText;
        message.confirmedAt = confirmedAt;
        return message;
    }
}
