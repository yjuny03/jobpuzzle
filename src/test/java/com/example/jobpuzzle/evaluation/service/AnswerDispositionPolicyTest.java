package com.example.jobpuzzle.evaluation.service;

import com.example.jobpuzzle.ai.dto.AnswerDisposition;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerDispositionPolicyTest {

    @Test
    void treatsAContractDeclaredUnrelatedAnswerAsRetryable() {
        assertThat(AnswerDispositionPolicy.resolve(
                AnswerDisposition.RETRY_ANSWER,
                5,
                Map.of("intentMatch", new AnswerEvaluation.DimensionEvaluation(5, "무관"))
        )).isEqualTo(AnswerDisposition.RETRY_ANSWER);
    }

    @Test
    void fallsBackToRetryWhenLegacyPromptReturnsOnlyNearZeroRelevantScores() {
        assertThat(AnswerDispositionPolicy.resolve(
                null,
                5,
                Map.of(
                        "intentMatch", new AnswerEvaluation.DimensionEvaluation(5, "무관"),
                        "problemSolving", new AnswerEvaluation.DimensionEvaluation(5, "내용 없음")
                )
        )).isEqualTo(AnswerDisposition.RETRY_ANSWER);
    }

    @Test
    void keepsAWeakButRelevantAnswerInTheFollowUpFlow() {
        assertThat(AnswerDispositionPolicy.resolve(
                null,
                18,
                Map.of(
                        "specificity", new AnswerEvaluation.DimensionEvaluation(10, "근거 부족"),
                        "deliveryClarity", new AnswerEvaluation.DimensionEvaluation(30, "이해 가능")
                )
        )).isEqualTo(AnswerDisposition.FOLLOW_UP);
    }

    @Test
    void overridesAnOvereagerRetryDeclarationWhenTheAnswerWasActuallyScorable() {
        assertThat(AnswerDispositionPolicy.resolve(
                AnswerDisposition.RETRY_ANSWER,
                18,
                Map.of(
                        "specificity", new AnswerEvaluation.DimensionEvaluation(10, "근거 부족"),
                        "problemSolving", new AnswerEvaluation.DimensionEvaluation(15, "노력 설명 부족"),
                        "deliveryClarity", new AnswerEvaluation.DimensionEvaluation(30, "문장은 이해 가능")
                )
        )).isEqualTo(AnswerDisposition.FOLLOW_UP);
    }
}
