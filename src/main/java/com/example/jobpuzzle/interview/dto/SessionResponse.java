package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.InterviewSessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionResponse {
    private Long sessionId;
    private InterviewSessionMode mode;
    private InterviewSessionStatus status;
    private Long questionSetId;
    private Long jobCategoryId;
    private String careerLevel;
    private int questionCount;
    private Long guideId;
    private String guideVersion;
    private String targetWeaknessTag;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;

    public static SessionResponse from(InterviewSession session, int questionCount) {
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .mode(session.getMode())
                .status(session.getStatus())
                .questionSetId(session.getQuestionSet().getQuestionSetId())
                .jobCategoryId(session.getJobCategory().getJobCategoryId())
                .careerLevel(session.getCareerLevel().name())
                .questionCount(questionCount)
                .guideId(session.getGuideDocument() == null ? null : session.getGuideDocument().getGuideId())
                .guideVersion(session.getGuideVersion())
                .targetWeaknessTag(session.getTargetWeaknessTag())
                .createdAt(session.getCreatedAt())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .canceledAt(session.getCanceledAt())
                .build();
    }
}
