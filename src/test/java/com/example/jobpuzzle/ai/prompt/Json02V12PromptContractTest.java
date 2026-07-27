package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class Json02V12PromptContractTest {
    @Test
    void forcesUnselectedDocumentObjectsToNullWithinEachPartition() throws Exception {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/json-02-v1.2.txt"));
        assertThat(prompt).contains("PARTITION_BOUNDARY_RULES", "반드시 JSON null", "빈 배열 객체", "이 규칙을 위반한 JSON은 무효다.");
    }
}
