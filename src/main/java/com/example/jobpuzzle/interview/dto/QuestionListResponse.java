package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.InterviewQuestion;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionListResponse {
    private Long questionId;
    private String questionType;
    private String question;
    private String relatedRequirementId;
    private String reviewStatus;
    private int displayOrder;
    private Long originSessionId;
    private String originMode;
    private Integer originScore;
    private Object originDiagnostics;

    public static QuestionListResponse from(InterviewQuestion value) {
        return QuestionListResponse.builder()
                .questionId(value.getQuestionId())
                .questionType(value.getQuestionType().name())
                .question(value.getQuestion())
                .relatedRequirementId(value.getRelatedRequirementId())
                .reviewStatus(value.getReviewStatus().name())
                .displayOrder(value.getDisplayOrder())
                .originSessionId(value.getOriginEvaluation() == null
                        ? null
                        : value.getOriginEvaluation().getSessionQuestion().getSession().getSessionId())
                .originMode(value.getOriginEvaluation() == null
                        ? null
                        : value.getOriginEvaluation().getEvaluationMode().name())
                .originScore(value.getOriginEvaluation() == null
                        ? null
                        : value.getOriginEvaluation().getScore())
                .originDiagnostics(value.getOriginEvaluation() == null
                        ? java.util.List.of()
                        : value.getOriginEvaluation().getWeaknessTags())
                .build();
    }
}
