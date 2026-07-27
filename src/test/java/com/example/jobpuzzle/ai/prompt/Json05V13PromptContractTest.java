package com.example.jobpuzzle.ai.prompt;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class Json05V13PromptContractTest {
    @Test
    void keepsProviderResponsibilityLimitedToJudgmentAndEvidenceIds() throws Exception {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/json-05-v1.3.txt"));

        assertThat(prompt).contains("candidateEvidenceIds", "evidenceIds", "SERVER_OWNED_FIELDS")
                .doesNotContain("{{jobPostingAnalysisJson}}", "{{candidateMaterialAnalysisJson}}",
                        "{{retrievedEvidenceJson}}", "{{guideContextJson}}");
        assertThat(prompt.split("\\Q{{synthesisInputJson}}\\E", -1)).hasSize(2);
        assertThat(prompt.substring(prompt.indexOf("[OUTPUT_SHAPE]")))
                .doesNotContain("\"sourceRefs\"", "\"extractionId\"", "\"documentId\"",
                        "\"matchId\"", "\"questionId\"", "\"taskId\"", "\"reviewStatus\"");
    }

    @Test
    void v14DefinesEveryConditionalCollectionContractBeforeClaudeCall() throws Exception {
        String prompt = Files.readString(Path.of("src/main/resources/prompts/json-05-v1.4.txt"));

        assertThat(prompt)
                .contains("questionGenerationEnabled=true이면 questions를 반드시 1개 이상 10개 이하",
                        "questionGenerationEnabled=false이면 questions를 반드시 []",
                        "HIGH가 아닌 각 requirement에는 task를 정확히 1개",
                        "postingEvidenceIds에서 1개 이상",
                        "allowedCandidateEvidenceIds가 비어 있지 않으면 그 목록에서도 1개 이상",
                        "ID 배열과 evaluationFocus에 중복 값이 없다",
                        "구조적 limitations는 서버가 계산한다")
                .doesNotContain("{{jobPostingAnalysisJson}}", "{{candidateMaterialAnalysisJson}}",
                        "{{retrievedEvidenceJson}}", "{{guideContextJson}}");
        assertThat(prompt.split("\\Q{{synthesisInputJson}}\\E", -1)).hasSize(2);
    }
}
