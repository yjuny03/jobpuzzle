package com.example.jobpuzzle.evaluation.service;

import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import java.util.Locale;
import java.util.UUID;

final class EvaluationFailureDiagnostics {

    private EvaluationFailureDiagnostics() {
    }

    static String newTraceId() {
        return "EVAL-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    }

    static String rootCauseSummary(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage() == null ? "(no message)" : root.getMessage();
        message = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (message.length() > 300) {
            message = message.substring(0, 300);
        }
        return root.getClass().getSimpleName() + ": " + message;
    }

    static AiCallLogErrorType errorType(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof AiProcessingException processing) {
                return processing.getErrorType();
            }
            current = current.getCause();
        }
        return AiCallLogErrorType.PROVIDER_ERROR;
    }
}
