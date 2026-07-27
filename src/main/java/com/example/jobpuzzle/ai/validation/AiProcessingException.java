package com.example.jobpuzzle.ai.validation;

import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.client.AiProviderCompletionMetadata;
import lombok.Getter;

@Getter
public class AiProcessingException extends RuntimeException {

    private final AiCallLogErrorType errorType;
    private final AiProviderCompletionMetadata completionMetadata;

    public AiProcessingException(AiCallLogErrorType errorType, String message) {
        this(errorType, message, null);
    }

    public AiProcessingException(AiCallLogErrorType errorType, String message,
                                 AiProviderCompletionMetadata completionMetadata) {
        super(message);
        this.errorType = errorType;
        this.completionMetadata = completionMetadata;
    }
}
