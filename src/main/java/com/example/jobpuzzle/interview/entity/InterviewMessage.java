package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 질문별 채팅형 메시지 기록.
// 원 질문·원 답변·꼬리질문·꼬리답변을 명시적으로 구분
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interview_message")
public class InterviewMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    // 소속 세션 질문
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_question_id", nullable = false)
    private InterviewSessionQuestion sessionQuestion;

    // 꼬리질문·꼬리답변의 기준 메시지
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
    @Column(
            name = "message_text",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String messageText;

    // USER 답변 완료 확정 시각. 질문 메시지는 null
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    // 세션 질문의 최초 원 질문 메시지 생성
    public static InterviewMessage originalQuestion(
            InterviewSessionQuestion sessionQuestion
    ) {
        return create(
                sessionQuestion,
                null,
                InterviewMessageSender.AI,
                InterviewMessageType.ORIGINAL_QUESTION,
                sessionQuestion.getQuestionTextSnapshot(),
                null
        );
    }

    // 사용자의 원 답변 또는 꼬리답변 메시지 생성
    public static InterviewMessage answer(
            InterviewSessionQuestion sessionQuestion,
            InterviewMessage parentQuestion,
            AnswerType answerType,
            String messageText
    ) {
        InterviewMessageType messageType =
                answerType == AnswerType.ORIGINAL_ANSWER
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

    // 평가 결과에 따라 AI 꼬리질문 메시지 생성
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

    // 별도 작성 후 확정하는 흐름이 추가될 경우 사용하는 호환 메서드
    public void confirm() {
        if (sender == InterviewMessageSender.USER
                && confirmedAt == null) {
            confirmedAt = LocalDateTime.now();
        }
    }

    public void rejectAnswer() {
        if (sender == InterviewMessageSender.USER
                && (messageType == InterviewMessageType.ORIGINAL_ANSWER
                || messageType == InterviewMessageType.FOLLOW_UP_ANSWER)) {
            messageType = InterviewMessageType.REJECTED_ANSWER;
        }
    }

    public void markEvaluationFailed() {
        if (sender == InterviewMessageSender.USER
                && (messageType == InterviewMessageType.ORIGINAL_ANSWER
                || messageType == InterviewMessageType.FOLLOW_UP_ANSWER)) {
            messageType = InterviewMessageType.EVALUATION_FAILED_ANSWER;
        }
    }
}
