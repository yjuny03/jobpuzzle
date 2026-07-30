package com.example.jobpuzzle.evaluation.service;

import com.example.jobpuzzle.ai.dto.AnswerDisposition;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;

import java.util.Map;

final class AnswerDispositionPolicy {

    private static final int PASS_THRESHOLD = 70;
    private static final int LEGACY_RETRY_MAX_SCORE = 10;

    private AnswerDispositionPolicy() {
    }

    static AnswerDisposition resolve(
            AnswerDisposition declared,
            int score,
            Map<String, AnswerEvaluation.DimensionEvaluation> details
    ) {
        boolean everyEvaluatedDimensionIsNearZero = !details.isEmpty()
                && details.values().stream()
                .map(AnswerEvaluation.DimensionEvaluation::getScore)
                .allMatch(value -> value != null && value <= LEGACY_RETRY_MAX_SCORE);
        if (declared == AnswerDisposition.RETRY_ANSWER
                && score <= LEGACY_RETRY_MAX_SCORE
                && everyEvaluatedDimensionIsNearZero) {
            return AnswerDisposition.RETRY_ANSWER;
        }
        if (declared == AnswerDisposition.EVALUATE) {
            return AnswerDisposition.EVALUATE;
        }
        if (declared == AnswerDisposition.FOLLOW_UP) {
            return score < PASS_THRESHOLD
                    ? AnswerDisposition.FOLLOW_UP
                    : AnswerDisposition.EVALUATE;
        }
        if (declared == null
                && score <= LEGACY_RETRY_MAX_SCORE
                && everyEvaluatedDimensionIsNearZero) {
            return AnswerDisposition.RETRY_ANSWER;
        }
        return score < PASS_THRESHOLD ? AnswerDisposition.FOLLOW_UP : AnswerDisposition.EVALUATE;
    }
}
