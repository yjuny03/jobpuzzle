package com.example.jobpuzzle.ai.validation;

import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import lombok.Getter;

@Getter
public class AiProcessingException extends RuntimeException {

    private final AiCallLogErrorType errorType;

    public AiProcessingException(AiCallLogErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
}
