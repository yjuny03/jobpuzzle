package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateDeploymentArtifactTest {

    @Test
    void json01SqlTemplateTextMatchesCanonicalPromptFile() throws IOException {
        // 배포 SQL에 담긴 JSON-01 본문이 리소스 정본과 달라지지 않게 검증한다.
        assertThat(sqlLiteral("insert-json-01-v1.0.sql", "PT-JOB-001"))
                .isEqualTo(prompt("json-01-v1.0.txt"));
    }

    @Test
    void json02SqlTemplateTextMatchesCanonicalPromptFile() throws IOException {
        // 배포 SQL에 담긴 JSON-02 본문이 리소스 정본과 달라지지 않게 검증한다.
        assertThat(sqlLiteral("insert-json-02-v1.0.sql", "PT-CAND-001"))
                .isEqualTo(prompt("json-02-v1.0.txt"));
    }

    private String prompt(String filename) throws IOException {
        // UTF-8 정본의 마지막 줄바꿈 차이는 SQL literal 비교에서 제거한다.
        return Files.readString(Path.of("src/main/resources/prompts", filename)).trim();
    }

    private String sqlLiteral(String filename, String promptCode) throws IOException {
        // INSERT VALUES의 다섯 번째 문자열(template_text)을 SQL quote 규칙에 맞춰 추출한다.
        String sql = Files.readString(Path.of("src/main/resources/db/manual", filename));
        int row = sql.lastIndexOf("'" + promptCode + "'");
        assertThat(row).isGreaterThanOrEqualTo(0);
        int cursor = row;
        Literal value = null;
        for (int index = 0; index < 5; index++) {
            value = nextLiteral(sql, cursor);
            cursor = value.end();
        }
        return value.value();
    }

    private Literal nextLiteral(String sql, int from) {
        int start = sql.indexOf('\'', from);
        int end = start + 1;
        StringBuilder value = new StringBuilder();
        while (end < sql.length()) {
            char current = sql.charAt(end++);
            if (current != '\'') value.append(current);
            else if (end < sql.length() && sql.charAt(end) == '\'') {
                value.append('\'');
                end++;
            } else return new Literal(value.toString(), end);
        }
        throw new AssertionError("unterminated SQL literal");
    }

    private record Literal(String value, int end) {
    }
}
