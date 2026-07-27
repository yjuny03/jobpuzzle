package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Json02V13PromptContractTest {

    @Test
    void preservesV11DtoAndEvidenceContractAndAddsPartitionBoundaries() throws IOException {
        String v11 = Files.readString(Path.of("src/main/resources/prompts/json-02-v1.1.txt"));
        String v13 = Files.readString(Path.of("src/main/resources/prompts/json-02-v1.3.txt"));

        assertThat(normalizeNewlines(v13.replace(partitionBoundaryRules(), "")).stripTrailing())
                .isEqualTo(normalizeNewlines(v11).stripTrailing());
        assertThat(v13)
                .contains("[PARTITION_BOUNDARY_RULES]")
                .contains("이번 요청에서 실제 marker가 제공된 문서 유형만 분석한다.")
                .contains("availableDocumentTypes에 없는 유형의 객체는 반드시 JSON null이다.")
                .contains("RESUME만 제공되면 coverLetter, portfolio, experienceNote는 반드시 null")
                .contains("이 규칙을 위반한 JSON은 무효다.")
                .contains("각 사실 항목은 원칙적으로 대표 marker 하나만 sourceRefs에 넣는다.")
                .contains("evidenceText 필드는 반환하지 않는다.")
                .contains("배열의 개수 고정 상한을 적용하지 않는다.")
                .contains("220자 이내")
                .contains("coverLetter의 motivation, values, jobConnection은 null 가능하다.")
                .contains("portfolio는 projects 배열을 가진다.")
                .contains("experienceNote는 starCandidates 배열을 가진다.");
    }

    private String partitionBoundaryRules() {
        return """
                [PARTITION_BOUNDARY_RULES]
                이번 요청에서 실제 marker가 제공된 문서 유형만 분석한다.
                다른 partition 또는 입력에 없는 문서의 사실·객체·sourceRefs를 생성하지 않는다.
                availableDocumentTypes에는 이번 요청의 RESUME, COVER_LETTER, PORTFOLIO, EXPERIENCE_NOTE만 중복 없이 넣는다.
                availableDocumentTypes에 없는 유형의 객체는 반드시 JSON null이다.
                예: RESUME만 제공되면 coverLetter, portfolio, experienceNote는 반드시 null이며 {} 또는 빈 배열 객체로 대체하지 않는다.
                이 규칙을 위반한 JSON은 무효다.

                """;
    }

    private String normalizeNewlines(String value) {
        return value.replace("\r\n", "\n");
    }
}
