package com.example.jobpuzzle.analysis.controller;

import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.ai.service.AiClientService;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 실제 사용자 흐름 그대로(PDF 업로드 → 텍스트 추출 → 확정 → 분석 작업 생성 → 자료 연결 → 입력 확정 → 색인 → /run)
 * 실제 Claude·OpenAI·Qdrant로 분석 1건을 실행하고 시간을 잰다.
 *
 * 유료 API를 호출하므로 평소에는 실행되지 않는다.
 * 실행: REAL_AI_MEASURE=true REAL_USER_ID=1 REAL_POSTING_PDF=... REAL_RESUME_PDF=... ./gradlew test --tests '*RealUserFlowMeasurementTest'
 */
@SpringBootTest(properties = {
        "app.ai.generation.default-provider=anthropic",
        "app.ai.generation.anthropic.base-url=https://api.anthropic.com"
})
@AutoConfigureMockMvc
@ActiveProfiles({"local", "ai-e2e"})
@EnabledIfEnvironmentVariable(named = "REAL_AI_MEASURE", matches = "true")
class RealUserFlowMeasurementTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private AiClientService aiClientService;

    private RequestPostProcessor auth;

    @Test
    void 공고와_이력서_PDF로_실제_분석_전체_흐름_측정() throws Exception {
        long userId = Long.parseLong(System.getenv("REAL_USER_ID"));
        User user = transactionTemplate.execute(status -> userRepository.findById(userId).orElseThrow());
        auth = SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(user));

        for (AiExecutionStage stage : List.of(AiExecutionStage.JOB_POSTING_ANALYSIS,
                AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS, AiExecutionStage.CUSTOMIZED_SYNTHESIS)) {
            assertThat(aiClientService.resolve(stage).provider()).as(stage + " provider").isEqualTo(AiProvider.ANTHROPIC);
        }

        // 1) 자료 등록·추출·확정
        long t = System.nanoTime();
        long postingExtraction = uploadExtractConfirm(Path.of(System.getenv("REAL_POSTING_PDF")), "JOB_POSTING", "가상 채용공고");
        long resumeExtraction = uploadExtractConfirm(Path.of(System.getenv("REAL_RESUME_PDF")), "RESUME", "유준영 이력서");
        long documentsMs = ms(t);

        // 2) 분석 작업 생성(백엔드 개발·신입) → 자료 연결 → 입력 확정
        JsonNode created = data(mockMvc.perform(post("/analysis-cases").with(auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"jobCategoryId\":" + jobCategoryId() + "}")).andReturn());
        long caseId = created.path("analysisCaseId").asLong();
        for (long extractionId : List.of(postingExtraction, resumeExtraction)) {
            mockMvc.perform(post("/analysis-cases/{id}/sources", caseId).with(auth)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"extractionId\":" + extractionId + "}")).andReturn();
        }
        JsonNode snapshot = data(mockMvc.perform(post("/analysis-cases/{id}/confirm", caseId).with(auth)).andReturn());

        // 3) 색인 → /run
        t = System.nanoTime();
        MvcResult index = mockMvc.perform(post("/analysis-cases/{id}/index", caseId).with(auth)).andReturn();
        long indexMs = ms(t);
        long runStart = System.nanoTime();
        MvcResult run = mockMvc.perform(post("/analysis/cases/{id}/run", caseId).with(auth)).andReturn();
        long runMs = ms(runStart);

        // 4) 완료까지 /status 폴링
        List<String> timeline = new ArrayList<>();
        String last = "";
        long maxStatusMs = 0;
        int statusCalls = 0;
        JsonNode lastData = null;
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15);
        while (System.currentTimeMillis() < deadline) {
            long s = System.nanoTime();
            JsonNode d = data(mockMvc.perform(get("/analysis/cases/{id}/status", caseId).with(auth)).andReturn());
            maxStatusMs = Math.max(maxStatusMs, ms(s));
            statusCalls++;
            lastData = d;
            String sig = String.join(" / ", d.path("analysisCaseStatus").asText(),
                    "공고 " + d.path("jobPostingAnalysisStatus").asText(),
                    "자료 " + d.path("candidateMaterialAnalysisStatus").asText(),
                    "가이드 " + d.path("guideContextStatus").asText(),
                    "맞춤 " + d.path("customizedAnalysisStatus").asText());
            if (!sig.equals(last)) {
                timeline.add(String.format("%6.1fs  %s", (System.nanoTime() - runStart) / 1e9, sig));
                last = sig;
            }
            if (!"ANALYZING".equals(d.path("analysisCaseStatus").asText())) break;
            Thread.sleep(2_000);
        }
        long totalMs = ms(runStart);

        System.out.println("\n===== 실제 사용자 흐름 측정 =====");
        System.out.printf("case=%d, snapshot=%s, 자료 등록·추출·확정 2건: %,d ms%n", caseId, snapshot.path("snapshotId").asText(), documentsMs);
        System.out.printf("[색인] HTTP %d, %,d ms%n", index.getResponse().getStatus(), indexMs);
        System.out.printf("[/run] HTTP %d, %,d ms%n", run.getResponse().getStatus(), runMs);
        timeline.forEach(System.out::println);
        System.out.printf("분석 완료까지(= 수정 전이었다면 /run이 붙잡았을 시간): %,d ms (%.1f분)%n", totalMs, totalMs / 60000.0);
        System.out.printf("분석 중 /status %d회, 최대 응답 %,d ms%n", statusCalls, maxStatusMs);
        System.out.println("최종 상태: " + (lastData == null ? "?" : lastData.path("analysisCaseStatus").asText()));
        if (lastData != null && !"COMPLETED".equals(lastData.path("analysisCaseStatus").asText())) {
            System.out.println("실패 정보: stage=" + lastData.path("latestFailureStage").asText()
                    + ", type=" + lastData.path("latestFailureType").asText() + ", code=" + lastData.path("errorCode").asText());
        }
        System.out.println();
    }

    private long uploadExtractConfirm(Path pdf, String documentType, String displayName) throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", pdf.getFileName().toString(),
                MediaType.APPLICATION_PDF_VALUE, Files.readAllBytes(pdf));
        JsonNode document = data(mockMvc.perform(multipart("/documents").file(file).with(auth)
                .param("documentType", documentType).param("displayName", displayName)).andReturn());
        long documentId = document.path("documentId").asLong();

        // 추출은 비동기라 접수 응답에는 추출 버전이 아직 없다. 자료의 버전 목록에 결과가 생길 때까지 기다린다.
        data(mockMvc.perform(post("/documents/{id}/extractions", documentId).with(auth)).andReturn());
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3);
        JsonNode version = null;
        while (System.currentTimeMillis() < deadline) {
            JsonNode versions = data(mockMvc.perform(get("/documents/{id}/extractions", documentId).with(auth)).andReturn());
            for (JsonNode v : versions) {
                if (List.of("SUCCESS", "FAILED", "PARTIAL").contains(v.path("extractionStatus").asText())) version = v;
            }
            if (version != null) break;
            Thread.sleep(500);
        }
        assertThat(version).as(documentType + " 추출 결과").isNotNull();
        long extractionId = version.path("extractionId").asLong();
        System.out.printf("[%s] document=%d extraction=%d status=%s pages=%s ocr=%s chars=%d%n", documentType, documentId,
                extractionId, version.path("extractionStatus").asText(), version.path("pageCount").asText(),
                version.path("ocrApplied").asText(), version.path("content").asText().length());

        mockMvc.perform(post("/document-extractions/{id}/confirm", extractionId).with(auth)).andReturn();
        return extractionId;
    }

    private JsonNode data(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(body.isBlank() ? "{}" : body);
        if (result.getResponse().getStatus() >= 400) {
            throw new IllegalStateException(result.getRequest().getRequestURI() + " → HTTP "
                    + result.getResponse().getStatus() + " " + root.path("code").asText() + " " + root.path("message").asText());
        }
        return root.path("data");
    }

    private static long ms(long beginNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beginNanos);
    }

    // 분석 기준 직무·경력 (기본 1 = IT·개발 > 백엔드 개발 > 신입). 활성 가이드가 있는 직무여야 분석이 시작된다.
    private static long jobCategoryId() {
        String value = System.getenv("REAL_JOB_CATEGORY_ID");
        return value == null || value.isBlank() ? 1L : Long.parseLong(value);
    }
}
