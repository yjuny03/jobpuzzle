package com.example.jobpuzzle.ai.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisSourceMarkerParserTest {

    private final AnalysisSourceMarkerParser parser = new AnalysisSourceMarkerParser();

    @Test
    void excludesJson01PromptContractSectionsFromTheDocumentMarkerText() {
        String prompt = """
                [JOB_POSTING_INPUT]
                [JOB_POSTING]
                [SOURCE extractionId=1 documentId=2][PAGE=1][SEGMENT=seg-001]
                Java와 Spring Boot 기반 백엔드 개발자를 찾습니다.

                [COMPANY_INFO_INPUT]

                [SOURCE_REFERENCE_RULES]
                marker 값은 변경 없이 echo한다.

                [OUTPUT_CONTRACT]
                JSON만 반환한다.
                """;

        var markers = parser.parsePrompt(prompt);

        assertThat(markers).singleElement().satisfies(marker -> {
            assertThat(marker.segmentText()).isEqualTo("Java와 Spring Boot 기반 백엔드 개발자를 찾습니다.");
            assertThat(marker.segmentText()).doesNotContain("SOURCE_REFERENCE_RULES", "OUTPUT_CONTRACT");
        });
    }
}
