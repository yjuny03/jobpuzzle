package com.example.jobpuzzle.evaluation.service;

import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationFailureDiagnosticsTest {

    @Test
    void reportsDeepestCauseWithoutLeakingLineBreaks() {
        RuntimeException failure = new IllegalStateException(
                "AI answer evaluation failed",
                new IllegalArgumentException("JSON contract mismatch\nraw payload")
        );

        assertThat(EvaluationFailureDiagnostics.rootCauseSummary(failure))
                .isEqualTo("IllegalArgumentException: JSON contract mismatch raw payload");
    }

    @Test
    void createsSearchableEvaluationTraceId() {
        assertThat(EvaluationFailureDiagnostics.newTraceId())
                .matches("EVAL-[A-Z0-9]{12}");
    }

    @Test
    void classifiesWrappedAiProcessingFailureByItsRealCause() {
        RuntimeException failure = new IllegalStateException(
                "AI answer evaluation failed",
                new AiProcessingException(AiCallLogErrorType.RESPONSE_VALIDATION_FAILED, "invalid schema")
        );

        assertThat(EvaluationFailureDiagnostics.errorType(failure))
                .isEqualTo(AiCallLogErrorType.RESPONSE_VALIDATION_FAILED);
    }
}
