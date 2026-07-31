package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class Json02V14PromptContractTest {
    @Test
    void preservesV13ContractAndAllowsOnlyNecessaryMultipleMarkerEvidence() throws Exception {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/before-json-02/json-02-v1.4.txt"));
        assertThat(prompt)
                .contains("[PARTITION_BOUNDARY_RULES]", "반드시 JSON null", "evidenceText 필드는 반환하지 않는다.")
                .contains("각 사실 항목의 sourceRefs는 반드시 비어 있지 않은 배열이다.")
                .contains("최소 개수의 marker를 같은 sourceRefs 배열에 함께 넣을 수 있다.")
                .contains("근거 없는 사실 객체의 대체물로 사용하지 않는다.")
                .contains("배열의 개수 고정 상한을 적용하지 않는다.", "220자 이내")
                .contains("portfolio는 projects 배열을 가진다.", "experienceNote는 starCandidates 배열을 가진다.");
    }
}
