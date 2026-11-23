package com.example.jobpuzzle.analysis.controller;

import com.example.jobpuzzle.ai.client.AiClient;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseSource;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseSourceRepository;
import com.example.jobpuzzle.analysis.service.AnalysisCaseService;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentExtractionStatus;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.document.repository.DocumentExtractionRepository;
import com.example.jobpuzzle.document.repository.UserDocumentRepository;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import com.example.jobpuzzle.guide.entity.GuideScopeType;
import com.example.jobpuzzle.guide.entity.JobGuideChunk;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 분석 실행 요청(/run)이 요청 스레드를 얼마나 오래 붙잡는지 측정한다.
 *
 * 실제 AI 대신 Mock AI에 호출마다 지연을 넣어 "AI 응답이 느린 상황"을 만들고,
 * /run 응답 시간과 분석이 실제로 끝나는 시간(/status COMPLETED)을 따로 잰다.
 * Docker 없이 로컬 MariaDB의 별도 스키마(jobpuzzle_async_test)를 쓰며, 테스트가 끝나면 테이블을 지운다.
 */
@SpringBootTest(properties = {
        "app.rag.mode=fake",
        "spring.datasource.url=jdbc:mariadb://localhost:3306/jobpuzzle_async_test?createDatabaseIfNotExist=true",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("db-integration")
// 로컬 MariaDB(localhost:3306)가 필요한 측정 테스트라 평소 테스트·CI에서는 실행하지 않는다. 실행: LOCAL_DB_MEASURE=true
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "LOCAL_DB_MEASURE", matches = "true")
class AnalysisRunLatencyMeasurementTest {

    /** Mock AI 호출 1번당 넣는 지연. 실제 Claude는 단계별 수십 초 */
    static final long AI_DELAY_MS = 1_500;
    static final AtomicInteger AI_CALLS = new AtomicInteger();

    @TestConfiguration
    static class SlowMockAiConfig {
        // AiClientService가 @Qualifier("mockAiClient")로 받는 Mock AI를, 호출마다 지연되는 프록시로 감싼다.
        @Bean
        static BeanPostProcessor slowMockAiClient() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (!"mockAiClient".equals(beanName) || !(bean instanceof AiClient)) return bean;
                    return Proxy.newProxyInstance(AiClient.class.getClassLoader(), new Class<?>[]{AiClient.class},
                            (proxy, method, args) -> {
                                String name = method.getName();
                                if (name.startsWith("analyze") || name.startsWith("generate")) {
                                    AI_CALLS.incrementAndGet();
                                    Thread.sleep(AI_DELAY_MS);
                                }
                                try {
                                    return method.invoke(bean, args);
                                } catch (InvocationTargetException e) {
                                    throw e.getCause();
                                }
                            });
                }
            };
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JobCategoryRepository jobCategoryRepository;
    @Autowired private AnalysisCaseRepository analysisCaseRepository;
    @Autowired private AnalysisCaseSourceRepository analysisCaseSourceRepository;
    @Autowired private AnalysisCaseService analysisCaseService;
    @Autowired private PromptTemplateRepository promptTemplateRepository;
    @Autowired private UserDocumentRepository userDocumentRepository;
    @Autowired private DocumentExtractionRepository extractionRepository;
    @Autowired private JobGuideDocumentRepository guideDocumentRepository;
    @Autowired private JobGuideChunkRepository guideChunkRepository;

    private static final int CONCURRENT_RUNS = 5;

    @Test
    void 분석_요청_동시_5건의_run_응답시간과_완료시간() throws Exception {
        // 실행기는 프롬프트 버전으로 처리 경로를 고르므로, 파일과 같은 실제 버전으로 넣는다.
        savePrompt("JSON-01", "v1.3", "prompts/json-01-v1.3.txt");
        savePrompt("JSON-02", "v1.6", "prompts/json-02-v1.6.txt");
        savePrompt("JSON-05", "v1.10", "prompts/json-05-v1.10.txt");
        List<Fixture> fixtures = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_RUNS; i++) {
            Fixture fixture = fixture();
            analysisCaseService.confirmInput(fixture.user().getUserId(), fixture.analysisCase().getAnalysisCaseId());
            fixtures.add(fixture);
        }
        // 첫 요청의 초기화 시간(보안 필터·JIT 등)이 측정에 섞이지 않도록 한 번 미리 호출한다.
        mockMvc.perform(get("/analysis/cases/{caseId}/status", fixtures.get(0).analysisCase().getAnalysisCaseId())
                .with(authenticated(fixtures.get(0).user()))).andReturn();
        AI_CALLS.set(0);

        // 5건을 동시에 /run → 각 요청이 응답을 받기까지 걸린 시간 = 요청 스레드를 붙잡은 시간
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_RUNS);
        CountDownLatch start = new CountDownLatch(1);
        long testStart = System.nanoTime();
        List<Future<RunResult>> futures = new ArrayList<>();
        for (Fixture fixture : fixtures) {
            futures.add(pool.submit(() -> {
                start.await();
                long begin = System.nanoTime();
                MvcResult result = mockMvc.perform(post("/analysis/cases/{caseId}/run", fixture.analysisCase().getAnalysisCaseId())
                        .with(authenticated(fixture.user()))).andReturn();
                return new RunResult(result.getResponse().getStatus(), elapsedMs(begin));
            }));
        }
        start.countDown();
        List<RunResult> runs = new ArrayList<>();
        for (Future<RunResult> future : futures) runs.add(future.get(5, TimeUnit.MINUTES));
        pool.shutdown();
        long allRunsReturnedMs = elapsedMs(testStart);

        // 분석이 실제로 끝났는지는 /status로 확인 (비동기면 여기서 기다리게 된다)
        List<String> finalStatuses = new ArrayList<>();
        for (Fixture fixture : fixtures) finalStatuses.add(waitUntilFinished(fixture));
        long allCompletedMs = elapsedMs(testStart);

        long maxRunMs = runs.stream().mapToLong(RunResult::latencyMs).max().orElse(0);
        long avgRunMs = (long) runs.stream().mapToLong(RunResult::latencyMs).average().orElse(0);
        long requestThreadMs = runs.stream().mapToLong(RunResult::latencyMs).sum();

        System.out.println("\n===== /run 응답 시간 측정 (동시 " + CONCURRENT_RUNS + "건, Mock AI 호출당 " + AI_DELAY_MS + "ms 지연) =====");
        runs.forEach(r -> System.out.printf("  /run → HTTP %d, %,d ms%n", r.httpStatus(), r.latencyMs()));
        System.out.printf("/run 평균 %,d ms / 최대 %,d ms%n", avgRunMs, maxRunMs);
        System.out.printf("요청 스레드 점유 합계: %,d ms%n", requestThreadMs);
        System.out.printf("5건 모두 /run 응답까지: %,d ms / 5건 모두 분석 완료까지: %,d ms%n", allRunsReturnedMs, allCompletedMs);
        System.out.printf("Mock AI 호출 수: %d, 최종 상태: %s%n%n", AI_CALLS.get(), finalStatuses);

        assertThat(finalStatuses).containsOnly("COMPLETED");
        // /run은 분석을 기다리지 않고 접수만 하므로, AI 호출 1번보다도 빨리 응답해야 한다.
        assertThat(runs).allSatisfy(r -> assertThat(r.httpStatus()).isEqualTo(202));
        assertThat(maxRunMs).isLessThan(AI_DELAY_MS);
    }

    // ---------- 도우미 ----------

    private String waitUntilFinished(Fixture fixture) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3);
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mockMvc.perform(get("/analysis/cases/{caseId}/status", fixture.analysisCase().getAnalysisCaseId())
                    .with(authenticated(fixture.user()))).andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
            String status = body.path("data").path("analysisCaseStatus").asText();
            if (!"ANALYZING".equals(status) && !"INPUT_CONFIRMED".equals(status)) return status;
            Thread.sleep(200);
        }
        return "TIMEOUT";
    }

    private static long elapsedMs(long beginNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beginNanos);
    }

    private record RunResult(int httpStatus, long latencyMs) { }

    private record Fixture(User user, AnalysisCase analysisCase) { }

    private Fixture fixture() {
        String suffix = UUID.randomUUID().toString();
        JobCategory category = jobCategoryRepository.save(JobCategory.builder()
                .mainCategory("ASYNC").subCategory("BACKEND-" + suffix).careerLevel(JobCategoryCareerLevel.NEW).build());
        User user = userRepository.save(User.createLocalUser("async-" + suffix, "encoded", suffix + "@example.test", "ASYNC", category));
        AnalysisCase analysisCase = analysisCaseRepository.save(AnalysisCase.builder().user(user).jobCategory(category).build());
        saveSource(user, analysisCase, UserDocumentType.JOB_POSTING, "Java와 Spring Boot 기반 백엔드 개발자를 찾습니다.");
        saveSource(user, analysisCase, UserDocumentType.RESUME, "Java와 Spring Boot로 REST API를 구현한 백엔드 개발 경험이 있습니다.");
        saveActiveCategoryGuide(user, category, suffix);
        return new Fixture(user, analysisCase);
    }

    private void saveSource(User user, AnalysisCase analysisCase, UserDocumentType type, String content) {
        UserDocument document = userDocumentRepository.save(UserDocument.builder().user(user).documentType(type)
                .sourceType(UserDocumentSourceType.TEXT).displayName(type.name() + " 자료").keepOriginal(false).build());
        DocumentExtraction extraction = extractionRepository.save(DocumentExtraction.builder().document(document)
                .extractionStatus(DocumentExtractionStatus.SUCCESS).versionStatus(DocumentVersionStatus.CONFIRMED)
                .content("[1페이지]\n" + content).pageCount(1).build());
        analysisCaseSourceRepository.save(AnalysisCaseSource.builder().analysisCase(analysisCase)
                .extraction(extraction).documentType(type).build());
    }

    private void saveActiveCategoryGuide(User user, JobCategory category, String suffix) {
        JobGuideDocument guide = JobGuideDocument.builder().guideCode("GUIDE-" + suffix).scopeType(GuideScopeType.CATEGORY)
                .jobCategory(category).title("Backend guide").sourceType(JobGuideDocumentSourceType.DIRECT_INPUT)
                .version("v1.0").createdBy(user).applicableScope("백엔드 개발")
                .evaluationFocus(List.of("요구사항 연결")).evidenceRules(List.of("근거 확인"))
                .questionDirection(List.of("구체 경험 확인")).avoidQuestions(List.of("추상 질문")).build();
        guide.activate();
        guide = guideDocumentRepository.saveAndFlush(guide);
        guideChunkRepository.saveAndFlush(JobGuideChunk.builder().guide(guide).chunkIndex(0).title("평가 기준")
                .content("Java와 Spring Boot 경험의 구체적 근거를 평가한다.").contentSummary("백엔드 평가 기준").build());
    }

    private void savePrompt(String targetJson, String version, String resourcePath) throws java.io.IOException {
        String template = new org.springframework.core.io.ClassPathResource(resourcePath).getContentAsString(StandardCharsets.UTF_8);
        promptTemplateRepository.saveAndFlush(PromptTemplate.builder().promptCode("LAT-" + targetJson + "-" + UUID.randomUUID())
                .name(targetJson + " latency test").version(version).targetJson(targetJson).templateText(template)
                .forbiddenRules("JSON 이외의 응답 금지").isActive(true).build());
    }

    private RequestPostProcessor authenticated(User user) {
        return SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(user));
    }
}
