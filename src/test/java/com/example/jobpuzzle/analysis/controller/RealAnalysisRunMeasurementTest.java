package com.example.jobpuzzle.analysis.controller;

import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import com.example.jobpuzzle.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 실제 Claude·OpenAI 임베딩·Qdrant로 분석 1건을 실행해 /run 응답 시간과 실제 분석 시간을 잰다.
 *
 * 유료 API를 호출하므로 평소 테스트에서는 실행되지 않는다.
 * 실행: REAL_AI_MEASURE=true REAL_AI_CASE_ID=<입력 확정 케이스 ID> ./gradlew test --tests '*RealAnalysisRunMeasurementTest'
 * 로컬 개발 DB(jobpuzzle)의 해당 케이스가 실제로 분석된다.
 */
@SpringBootTest(properties = {
        // build.gradle이 테스트에서 AI_GENERATION_DEFAULT_PROVIDER=mock을 강제하므로, 이 측정에서만 실제 Claude로 바꾼다.
        "app.ai.generation.default-provider=anthropic",
        // 셸 환경변수 ANTHROPIC_BASE_URL에 끌려가지 않도록 공식 주소로 고정한다.
        "app.ai.generation.anthropic.base-url=https://api.anthropic.com"
})
@AutoConfigureMockMvc
@ActiveProfiles({"local", "ai-e2e"}) // ai-e2e: 실연동 E2E용 단계별 설정·JSON-05 진단 로그
@EnabledIfEnvironmentVariable(named = "REAL_AI_MEASURE", matches = "true")
class RealAnalysisRunMeasurementTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AnalysisCaseRepository analysisCaseRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private com.example.jobpuzzle.ai.service.AiClientService aiClientService;
    @Autowired private com.example.jobpuzzle.ai.config.AiGenerationProperties generationProperties;

    @Test
    void 실제_AI로_분석_1건_실행_측정() throws Exception {
        long caseId = Long.parseLong(System.getenv("REAL_AI_CASE_ID"));
        User user = transactionTemplate.execute(status -> {
            AnalysisCase analysisCase = analysisCaseRepository.findById(caseId).orElseThrow();
            // 케이스에 걸린 User는 지연 로딩 프록시라, 실제 엔티티로 꺼내 인증 정보에 쓴다.
            return (User) org.hibernate.Hibernate.unproxy(analysisCase.getUser());
        });
        RequestPostProcessor auth = SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(user));

        // 실제 Claude로 가는지 먼저 확인한다. Mock이면 측정 의미가 없으므로 /run 전에 멈춘다.
        for (com.example.jobpuzzle.ai.log.AiExecutionStage stage : List.of(
                com.example.jobpuzzle.ai.log.AiExecutionStage.JOB_POSTING_ANALYSIS,
                com.example.jobpuzzle.ai.log.AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS,
                com.example.jobpuzzle.ai.log.AiExecutionStage.CUSTOMIZED_SYNTHESIS)) {
            org.assertj.core.api.Assertions.assertThat(aiClientService.resolve(stage).provider())
                    .as(stage + " provider").isEqualTo(com.example.jobpuzzle.ai.log.AiProvider.ANTHROPIC);
        }
        System.out.println("[확인] 분석 3단계 모두 ANTHROPIC, base-url=" + generationProperties.getAnthropic().getBaseUrl());

        // 화면과 같은 순서: 색인 확인 → /run → /status 폴링
        long t0 = System.nanoTime();
        MvcResult index = mockMvc.perform(post("/analysis-cases/{id}/index", caseId).with(auth)).andReturn();
        long indexMs = ms(t0);

        long runStart = System.nanoTime();
        MvcResult run = mockMvc.perform(post("/analysis/cases/{id}/run", caseId).with(auth)).andReturn();
        long runMs = ms(runStart);
        System.out.printf("%n[색인] HTTP %d, %,d ms%n[/run] HTTP %d, %,d ms → %s%n",
                index.getResponse().getStatus(), indexMs, run.getResponse().getStatus(), runMs,
                run.getResponse().getContentAsString(StandardCharsets.UTF_8));

        // 분석이 끝날 때까지 /status 확인. 단계가 바뀔 때마다 경과 시간을 남기고, /status 응답 시간도 함께 잰다.
        List<String> timeline = new ArrayList<>();
        String lastSignature = "";
        long maxStatusMs = 0;
        int statusCalls = 0;
        String finalStatus = "TIMEOUT";
        JsonNode lastData = null;
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15);
        while (System.currentTimeMillis() < deadline) {
            long s = System.nanoTime();
            MvcResult status = mockMvc.perform(get("/analysis/cases/{id}/status", caseId).with(auth)).andReturn();
            long statusMs = ms(s);
            maxStatusMs = Math.max(maxStatusMs, statusMs);
            statusCalls++;
            JsonNode data = objectMapper.readTree(status.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
            lastData = data;
            String signature = String.join(" / ",
                    data.path("analysisCaseStatus").asText(),
                    "공고 " + data.path("jobPostingAnalysisStatus").asText(),
                    "자료 " + data.path("candidateMaterialAnalysisStatus").asText(),
                    "가이드 " + data.path("guideContextStatus").asText(),
                    "맞춤 " + data.path("customizedAnalysisStatus").asText());
            if (!signature.equals(lastSignature)) {
                timeline.add(String.format("%6.1fs  %s", (System.nanoTime() - runStart) / 1e9, signature));
                lastSignature = signature;
            }
            String caseStatus = data.path("analysisCaseStatus").asText();
            if (!"ANALYZING".equals(caseStatus)) {
                finalStatus = caseStatus;
                break;
            }
            Thread.sleep(2_000);
        }
        long totalMs = ms(runStart);

        System.out.println("\n===== 실제 AI 분석 1건 측정 (case " + caseId + ") =====");
        timeline.forEach(System.out::println);
        System.out.printf("/run 응답: %,d ms%n", runMs);
        System.out.printf("분석 완료까지(= 수정 전이었다면 /run이 붙잡았을 시간): %,d ms (%.1f분)%n", totalMs, totalMs / 60000.0);
        System.out.printf("분석 중 /status %d회 호출, 최대 응답 %,d ms%n", statusCalls, maxStatusMs);
        System.out.println("최종 상태: " + finalStatus);
        if (lastData != null && !"COMPLETED".equals(finalStatus)) {
            System.out.println("실패 정보: stage=" + lastData.path("latestFailureStage").asText()
                    + ", type=" + lastData.path("latestFailureType").asText()
                    + ", code=" + lastData.path("errorCode").asText());
        }
        System.out.println();
    }

    private static long ms(long beginNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beginNanos);
    }
}
