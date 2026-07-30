package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Json02V13PromptContractTest {

    @Test
    void preservesV11DtoAndEvidenceContractAndAddsPartitionBoundaries() throws IOException {
        String v11 = Files.readString(Path.of("src/main/resources/prompts/before-json-02/json-02-v1.1.txt"));
        String v13 = Files.readString(Path.of("src/main/resources/prompts/before-json-02/json-02-v1.3.txt"));

        assertThat(normalizeNewlines(removePartitionBoundaryRules(v13)).stripTrailing())
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

    private String removePartitionBoundaryRules(String value) {
        // 섹션 제목 경계로 추가 블록만 제거해 OS별 줄바꿈과 빈 줄 개수에 영향받지 않게 비교한다.
        return value.replaceFirst(
                "(?s)\\[PARTITION_BOUNDARY_RULES]\\R.*?\\R(?=\\[SOURCE_REFERENCE_RULES])",
                ""
        );
    }

    private String normalizeNewlines(String value) {
        return value.replace("\r\n", "\n");
    }
}
