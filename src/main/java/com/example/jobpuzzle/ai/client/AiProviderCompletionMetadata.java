package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.log.AiFailureKind;

import java.util.List;

/**
 * 원시 prompt·response·thinking을 보관하지 않는 호출 완료 메타데이터다.
 * JSON-02 partition의 재분할 판단과 안전한 E2E 진단에만 사용한다.
 */
public record AiProviderCompletionMetadata(
        String stopReason,
        List<String> contentBlockTypes,
        boolean textBlockPresent,
        Long inputTokens,
        Long outputTokens,
        String requestId,
        Boolean jsonObjectComplete,
        Boolean dtoParsed,
        Boolean sourceReferencesValid,
        Boolean adaptiveSplitEligible,
        AiFailureKind failureKind
) {
    public AiProviderCompletionMetadata {
        contentBlockTypes = contentBlockTypes == null ? List.of() : List.copyOf(contentBlockTypes);
        failureKind = failureKind == null ? AiFailureKind.NONE : failureKind;
    }

    public AiProviderCompletionMetadata withPostProcessing(Boolean jsonObjectComplete, Boolean dtoParsed,
                                                            Boolean sourceReferencesValid, Boolean adaptiveSplitEligible,
                                                            AiFailureKind failureKind) {
        return new AiProviderCompletionMetadata(stopReason, contentBlockTypes, textBlockPresent, inputTokens, outputTokens,
                requestId, jsonObjectComplete, dtoParsed, sourceReferencesValid, adaptiveSplitEligible, failureKind);
    }
}
