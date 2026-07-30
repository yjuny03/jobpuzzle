package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Json02V11PromptContractTest {

    @Test
    void preservesDistinctFactsWithoutArrayCapsAndUsesMarkerOnlyReferences() throws IOException {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/before-json-02/json-02-v1.1.txt"));

        assertThat(prompt)
                .contains("배열의 개수 고정 상한을 적용하지 않는다.")
                .contains("같은 marker를 사용해도 의미가 다른 경험·기술·역할·성과는 모두 유지한다.")
                .contains("각 사실 항목은 원칙적으로 대표 marker 하나만 sourceRefs에 넣는다.")
                .contains("evidenceText 필드는 반환하지 않는다.")
                .contains("220자 이내")
                .doesNotContain("\"evidenceText\"");
    }
}
