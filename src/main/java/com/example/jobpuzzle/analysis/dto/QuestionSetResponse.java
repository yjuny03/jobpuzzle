package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.interview.entity.InterviewQuestion;
import com.example.jobpuzzle.interview.entity.QuestionSet;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionSetResponse {
    private Long questionSetId;
    private String mode;
    private Long snapshotId;
    private Long jobCategoryId;
    private String careerLevel;
    private String generationSource;
    private String targetWeaknessTag;
    private String targetDimension;
    private String status;
    private List<Question> questions;

    public static QuestionSetResponse from(QuestionSet set, List<InterviewQuestion> questions) {
        return QuestionSetResponse.builder()
                .questionSetId(set.getQuestionSetId())
                .mode(set.getInterviewMode().name())
                .snapshotId(set.getSnapshot() == null ? null : set.getSnapshot().getSnapshotId())
                .jobCategoryId(set.getJobCategory().getJobCategoryId())
                .careerLevel(set.getCareerLevel().name())
                .generationSource(set.getGenerationSource().name())
                .targetWeaknessTag(set.getTargetWeaknessTag())
                .targetDimension(set.getTargetDimension())
                .status(set.getStatus().name())
                .questions(questions.stream().map(Question::from).toList())
                .build();
    }

    @Getter
    @Builder
    public static class Question {
        private Long questionId;
        private String questionType;
        private String question;
        private String intent;
        private Object evaluationFocus;
        private int displayOrder;

        private static Question from(InterviewQuestion value) {
            return Question.builder()
                    .questionId(value.getQuestionId())
                    .questionType(value.getQuestionType().name())
                    .question(value.getQuestion())
                    .intent(value.getIntent())
                    .evaluationFocus(value.getEvaluationFocus())
                    .displayOrder(value.getDisplayOrder())
                    .build();
        }
    }
}
