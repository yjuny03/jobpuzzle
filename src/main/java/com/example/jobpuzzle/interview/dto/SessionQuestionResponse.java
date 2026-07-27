package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import com.example.jobpuzzle.interview.entity.InterviewMessage;
import com.example.jobpuzzle.interview.entity.InterviewMessageSender;
import com.example.jobpuzzle.interview.entity.InterviewMessageType;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestionStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SessionQuestionResponse {
    private Long sessionQuestionId;
    private Long questionId;
    private String questionText;
    private String intent;
    private List<InterviewQuestionEvaluationFocus> evaluationFocus;
    private int displayOrder;
    private InterviewSessionQuestionStatus status;
    private boolean answerSubmitted;
    private int followUpCount;
    private Long pendingFollowUpMessageId;
    private String pendingFollowUpQuestion;
    private List<ConversationMessage> conversation;

    @Getter
    @Builder
    public static class ConversationMessage {
        private Long messageId;
        private InterviewMessageSender sender;
        private InterviewMessageType messageType;
        private String messageText;

        public static ConversationMessage from(InterviewMessage message) {
            return ConversationMessage.builder()
                    .messageId(message.getMessageId())
                    .sender(message.getSender())
                    .messageType(message.getMessageType())
                    .messageText(message.getMessageText())
                    .build();
        }
    }

    public static SessionQuestionResponse from(InterviewSessionQuestion question) {
        return SessionQuestionResponse.builder()
                .sessionQuestionId(question.getSessionQuestionId())
                .questionId(question.getQuestion().getQuestionId())
                .questionText(question.getQuestionTextSnapshot())
                .intent(question.getIntentSnapshot())
                .evaluationFocus(question.getEvaluationFocusSnapshot())
                .displayOrder(question.getDisplayOrder())
                .status(question.getStatus())
                .answerSubmitted(false)
                .followUpCount(0)
                .conversation(List.of())
                .build();
    }

    public static SessionQuestionResponse from(
            InterviewSessionQuestion question,
            boolean answerSubmitted,
            int followUpCount,
            Long pendingFollowUpMessageId,
            String pendingFollowUpQuestion,
            List<InterviewMessage> messages
    ) {
        return SessionQuestionResponse.builder()
                .sessionQuestionId(question.getSessionQuestionId())
                .questionId(question.getQuestion().getQuestionId())
                .questionText(question.getQuestionTextSnapshot())
                .intent(question.getIntentSnapshot())
                .evaluationFocus(question.getEvaluationFocusSnapshot())
                .displayOrder(question.getDisplayOrder())
                .status(question.getStatus())
                .answerSubmitted(answerSubmitted)
                .followUpCount(followUpCount)
                .pendingFollowUpMessageId(pendingFollowUpMessageId)
                .pendingFollowUpQuestion(pendingFollowUpQuestion)
                .conversation(messages.stream()
                        .map(ConversationMessage::from)
                        .toList())
                .build();
    }
}
