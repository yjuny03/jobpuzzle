package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Json02V15PromptContractTest {

    @Test
    void preservesEvidenceAndPartitionContractsWhileReservingTechnicalIdsForTheServer() throws Exception {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/json-02-v1.5.txt"));

        assertThat(prompt)
                .contains("[PARTITION_BOUNDARY_RULES]", "반드시 JSON null", "evidenceText 필드는 반환하지 않는다.")
                .contains("각 사실 항목의 sourceRefs는 반드시 비어 있지 않은 배열이다.")
                .contains("최소 개수의 marker를 같은 sourceRefs 배열에 함께 넣을 수 있다.")
                .contains("배열의 개수 고정 상한을 적용하지 않는다.", "220자 이내")
                .contains("experienceId는 서버가 부여하므로 반환하지 않는다.")
                .contains("projectId는 서버가 부여하므로 반환하지 않는다.")
                .contains("candidateId는 서버가 부여하므로 반환하지 않는다.");
    }
}
