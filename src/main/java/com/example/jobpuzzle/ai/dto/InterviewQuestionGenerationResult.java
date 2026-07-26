package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import com.example.jobpuzzle.interview.entity.InterviewQuestionReviewStatus;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JSON-09(약점 보완)와 JSON-11(기본 모드)의 공통 질문 생성 응답 계약.
 */
@Getter
@NoArgsConstructor
public class InterviewQuestionGenerationResult {

    private List<Question> questions;

    public InterviewQuestionGenerationResult(List<Question> questions) {
        this.questions = questions;
    }

    @Getter
    @NoArgsConstructor
    public static class Question {
        private String questionId;
        private InterviewQuestionType questionType;
        private String question;
        private String intent;
        private List<InterviewQuestionEvaluationFocus> evaluationFocus;
        private Long originEvaluationId;
        private String targetWeaknessTag;
        private String targetDimension;
        private InterviewQuestionReviewStatus reviewStatus;
        private String reviewNote;

        public Question(
                String questionId,
                InterviewQuestionType questionType,
                String question,
                String intent,
                List<InterviewQuestionEvaluationFocus> evaluationFocus,
                Long originEvaluationId,
                String targetWeaknessTag,
                String targetDimension,
                InterviewQuestionReviewStatus reviewStatus,
                String reviewNote
        ) {
            this.questionId = questionId;
            this.questionType = questionType;
            this.question = question;
            this.intent = intent;
            this.evaluationFocus = evaluationFocus;
            this.originEvaluationId = originEvaluationId;
            this.targetWeaknessTag = targetWeaknessTag;
            this.targetDimension = targetDimension;
            this.reviewStatus = reviewStatus;
            this.reviewNote = reviewNote;
        }
    }
}
