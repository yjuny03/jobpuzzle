package com.example.jobpuzzle.evaluation.dto;

import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnswerEvaluationResponse {
    private Long evaluationId;
    private Integer score;
    private Integer passThreshold;
    private String scoreLabel;
    private Object evaluationDetail;
    private Object weaknessTags;
    private String summary;
    private Object improvementDirection;
    private String targetWeaknessTag;
    private String targetDimension;

    public static AnswerEvaluationResponse from(AnswerEvaluation evaluation) {
        return AnswerEvaluationResponse.builder()
                .evaluationId(evaluation.getEvaluationId())
                .score(evaluation.getScore())
                .passThreshold(evaluation.getPassThreshold())
                .scoreLabel(evaluation.getScoreLabel())
                .evaluationDetail(evaluation.getEvaluationDetail())
                .weaknessTags(evaluation.getWeaknessTags())
                .summary(evaluation.getSummary())
                .improvementDirection(evaluation.getImprovementDirection())
                .targetWeaknessTag(evaluation.getTargetWeaknessTag())
                .targetDimension(evaluation.getTargetDimension())
                .build();
    }
}
