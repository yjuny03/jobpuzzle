package com.example.jobpuzzle.analysis.controller;

import com.example.jobpuzzle.ai.client.AiClient;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogRepository;
import com.example.jobpuzzle.ai.log.AiCallLogRole;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 자료 분석(JSON-02) 분할 호출을 동시에 실행했을 때 단계 시간이 얼마나 줄어드는지 잰다.
 *
 * 8쪽 이력서를 2쪽씩 나눠 분할 호출 4번이 생기게 하고(실제 실행과 같은 모양),
 * Mock AI 호출마다 1.5초 지연을 넣는다. 동시 실행 수는 MEASURE_JSON02_CONCURRENCY(기본 3)로 바꿔 비교한다.
 * 1이면 예전처럼 하나씩 순서대로 실행하는 것과 같다.
 */
@SpringBootTest(properties = {
        "app.rag.mode=fake",
        "spring.datasource.url=jdbc:mariadb://localhost:3306/jobpuzzle_async_test?createDatabaseIfNotExist=true",
        "spring.jpa.show-sql=false",
        "app.ai.generation.input-limits.json02-partition-max-markers=2",
        "app.ai.generation.input-limits.json02-partition-marker-content-chars=4000",
        "app.ai.generation.json02-partition-concurrency=${MEASURE_JSON02_CONCURRENCY:3}"
})
@AutoConfigureMockMvc
@ActiveProfiles("db-integration")
// 로컬 MariaDB(localhost:3306)가 필요한 측정 테스트라 평소 테스트·CI에서는 실행하지 않는다. 실행: LOCAL_DB_MEASURE=true
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "LOCAL_DB_MEASURE", matches = "true")
class Json02PartitionConcurrencyMeasurementTest {

    static final long AI_DELAY_MS = 1_500;

    @TestConfiguration
    static class SlowMockAiConfig {
        @Bean
        static BeanPostProcessor slowMockAiClient() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (!"mockAiClient".equals(beanName) || !(bean instanceof AiClient)) return bean;
                    return Proxy.newProxyInstance(AiClient.class.getClassLoader(), new Class<?>[]{AiClient.class},
                            (proxy, method, args) -> {
                                if (method.getName().startsWith("analyze") || method.getName().startsWith("generate")) {
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

    @Value("${app.ai.generation.json02-partition-concurrency}") private int concurrency;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JobCategoryRepository jobCategoryRepository;
    @Autowired private AnalysisCaseRepository analysisCaseRepository;
    @Autowired private AnalysisCaseSourceRepository analysisCaseSourceRepository;
    @Autowired private AnalysisCaseService analysisCaseService;
    @Autowired private UserDocumentRepository userDocumentRepository;
    @Autowired private DocumentExtractionRepository extractionRepository;
    @Autowired private PromptTemplateRepository promptTemplateRepository;
    @Autowired private JobGuideDocumentRepository guideDocumentRepository;
    @Autowired private JobGuideChunkRepository guideChunkRepository;
    @Autowired private AiCallLogRepository aiCallLogRepository;

    @Test
    void 이력서_8쪽_분할호출_4번의_자료분석_시간() throws Exception {
        savePrompt("JSON-01", "v1.3", "prompts/json-01-v1.3.txt");
        savePrompt("JSON-02", "v1.6", "prompts/json-02-v1.6.txt");
        savePrompt("JSON-05", "v1.10", "prompts/json-05-v1.10.txt");

        String suffix = UUID.randomUUID().toString();
        JobCategory category = jobCategoryRepository.save(JobCategory.builder()
                .mainCategory("PART").subCategory("BACKEND-" + suffix).careerLevel(JobCategoryCareerLevel.NEW).build());
        User user = userRepository.save(User.createLocalUser("part-" + suffix, "encoded", suffix + "@example.test", "PART", category));
        AnalysisCase analysisCase = analysisCaseRepository.save(AnalysisCase.builder().user(user).jobCategory(category).build());
        saveSource(user, analysisCase, UserDocumentType.JOB_POSTING,
                "[1페이지]\n[자격요건] Java와 Spring Boot 기반 백엔드 개발 경험\n[우대사항] JPA 트랜잭션과 동시성 제어 경험");
        // 8쪽 이력서: 2쪽씩 묶이면 분할 호출 4번
        String resume = IntStream.rangeClosed(1, 8)
                .mapToObj(page -> "[" + page + "페이지]\n" + page + "쪽: Java와 Spring Boot로 REST API를 구현하고 JPA로 데이터를 다룬 프로젝트 경험을 정리했습니다.")
                .collect(Collectors.joining("\n"));
        saveSource(user, analysisCase, UserDocumentType.RESUME, resume);
        saveGuide(user, category, suffix);
        analysisCaseService.confirmInput(user.getUserId(), analysisCase.getAnalysisCaseId());
        RequestPostProcessor auth = SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(user));

        long start = System.nanoTime();
        mockMvc.perform(post("/analysis/cases/{id}/run", analysisCase.getAnalysisCaseId()).with(auth)).andReturn();
        String status = "TIMEOUT";
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3);
        while (System.currentTimeMillis() < deadline) {
            status = objectMapper.readTree(mockMvc.perform(get("/analysis/cases/{id}/status", analysisCase.getAnalysisCaseId()).with(auth))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data").path("analysisCaseStatus").asText();
            if (!"ANALYZING".equals(status)) break;
            Thread.sleep(100);
        }
        long totalMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        String snapshotId = String.valueOf(analysisCaseService.getAnalysisContext(user.getUserId(), analysisCase.getAnalysisCaseId()).getSnapshotId());
        List<AiCallLog> json02Calls = aiCallLogRepository.findAll().stream()
                .filter(log -> snapshotId.equals(log.getInputReferenceId()))
                .filter(log -> log.getExecutionStage() == AiExecutionStage.CANDIDATE_MATERIAL_ANALYSIS)
                .filter(log -> log.getCallRole() == AiCallLogRole.PROVIDER_CALL)
                .toList();
        LocalDateTime first = json02Calls.stream().map(AiCallLog::getStartedAt).min(Comparator.naturalOrder()).orElseThrow();
        LocalDateTime last = json02Calls.stream().map(AiCallLog::getCompletedAt).max(Comparator.naturalOrder()).orElseThrow();
        long json02Ms = Duration.between(first, last).toMillis();

        System.out.println("\n===== 자료 분석(JSON-02) 분할 동시 실행 측정 (동시 " + concurrency + "개, Mock AI 호출당 " + AI_DELAY_MS + "ms) =====");
        System.out.printf("JSON-02 분할 호출 %d번, 자료 분석 단계 %,d ms%n", json02Calls.size(), json02Ms);
        System.out.printf("분석 전체 %,d ms, 최종 상태 %s%n%n", totalMs, status);

        assertThat(status).isEqualTo("COMPLETED");
        assertThat(json02Calls).hasSize(4);
    }

    private void saveSource(User user, AnalysisCase analysisCase, UserDocumentType type, String content) {
        UserDocument document = userDocumentRepository.save(UserDocument.builder().user(user).documentType(type)
                .sourceType(UserDocumentSourceType.TEXT).displayName(type.name() + " 자료").keepOriginal(false).build());
        DocumentExtraction extraction = extractionRepository.save(DocumentExtraction.builder().document(document)
                .extractionStatus(DocumentExtractionStatus.SUCCESS).versionStatus(DocumentVersionStatus.CONFIRMED)
                .content(content).pageCount(8).build());
        analysisCaseSourceRepository.save(AnalysisCaseSource.builder().analysisCase(analysisCase)
                .extraction(extraction).documentType(type).build());
    }

    private void saveGuide(User user, JobCategory category, String suffix) {
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
        String template = new ClassPathResource(resourcePath).getContentAsString(StandardCharsets.UTF_8);
        promptTemplateRepository.saveAndFlush(PromptTemplate.builder().promptCode("PART-" + targetJson + "-" + UUID.randomUUID())
                .name(targetJson + " partition test").version(version).targetJson(targetJson).templateText(template)
                .forbiddenRules("JSON 이외의 응답 금지").isActive(true).build());
    }
}
