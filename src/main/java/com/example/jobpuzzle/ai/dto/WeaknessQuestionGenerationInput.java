package com.example.jobpuzzle.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * JSON-09에 전달할 약점 보완 입력.
 */
public record WeaknessQuestionGenerationInput(
        String targetWeaknessTag,
        String targetDimension,
        List<OriginEvaluation> originEvaluations
) {
    public record OriginEvaluation(
            Long evaluationId,
            String originalQuestion,
            String answer,
            Integer score,
            Map<String, ?> evaluationDetail,
            String evaluationSummary
    ) {
    }
}
