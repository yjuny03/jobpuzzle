package com.example.jobpuzzle.guide.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class GuideDeploymentArtifactTest {

    private static final Pattern CHUNK = Pattern.compile("<!-- GUIDE_CHUNK index=(\\d+) title=\\\"([^\\\"]+)\\\" -->\\R([\\s\\S]*?)(?=\\R<!-- GUIDE_CHUNK|\\z)");

    @Test
    void sqlChunksMatchTheCanonicalGuideSectionsInOrder() throws IOException {
        // 정본 markdown의 각 chunk 본문과 순서가 최초 등록 SQL에 동일하게 포함되는지 검증한다.
        String guide = Files.readString(Path.of("src/main/resources/guides/backend-new-v1.0.md"));
        String sql = Files.readString(Path.of("src/main/resources/db/manual/insert-backend-new-guide-v1.0.sql"));
        String normalizedSql = normalizeIndentation(sql);
        Matcher matcher = CHUNK.matcher(guide);
        int expectedIndex = 0;
        while (matcher.find()) {
            String title = matcher.group(2);
            String content = matcher.group(3).trim();
            assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(expectedIndex++);
            assertThat(normalizedSql)
                    .contains("'" + title + "'", "'" + normalizeIndentation(content) + "'");
        }
        assertThat(expectedIndex).isEqualTo(2);
    }

    @Test
    void sqlTargetsTheCurrentBackendNewCategoryAndCreatesTwoChunks() throws IOException {
        // 개발 테스트 case의 CATEGORY 키와 예상 chunk 수가 SQL에 고정됐는지 검증한다.
        String sql = Files.readString(Path.of("src/main/resources/db/manual/insert-backend-new-guide-v1.0.sql"));
        assertThat(sql).contains("'CATEGORY'", "'CATEGORY', 1, NULL", "'ACTIVE'", "'DIRECT_INPUT'");
        assertThat(sql).contains("@backend_new_guide_id, 0", "@backend_new_guide_id, 1");
    }

    private String normalizeIndentation(String value) {
        return value.replace("\r\n", "\n").lines()
                .map(String::strip)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }
}
