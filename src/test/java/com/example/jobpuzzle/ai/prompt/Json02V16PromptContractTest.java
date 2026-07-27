package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Json02V16PromptContractTest {

    @Test
    void preservesPartitionAndEvidenceRulesWithARequiredPrimaryReference() throws Exception {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/json-02-v1.6.txt"));

        assertThat(prompt)
                .contains("[PARTITION_BOUNDARY_RULES]", "반드시 JSON null", "evidenceText 필드는 반환하지 않는다.")
                .contains("sourceRef 객체를 반드시 하나", "additionalSourceRefs 배열")
                .contains("sourceRef는 null 또는 빈 객체가 될 수 없고")
                .contains("최소 개수의 추가 marker", "배열의 개수 고정 상한을 적용하지 않는다.", "220자 이내")
                .contains("experienceId는 서버가 부여하므로 반환하지 않는다.");
    }
}
