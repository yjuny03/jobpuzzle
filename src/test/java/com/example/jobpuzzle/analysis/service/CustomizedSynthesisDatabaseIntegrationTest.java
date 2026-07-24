package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogRepository;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiInputReferenceType;
import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseStatus;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.entity.ReadinessResult;
import com.example.jobpuzzle.analysis.entity.ReadinessResultStatus;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.ReadinessResultRepository;
import com.example.jobpuzzle.guide.entity.GuideContextResult;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import com.example.jobpuzzle.guide.repository.GuideContextResultRepository;
import com.example.jobpuzzle.interview.repository.QuestionSetRepository;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/** MariaDB에서 JSON-05 저장기의 REQUIRES_NEW 경계를 검증하는 실제 DB 테스트 기반이다. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("db-integration")
@Testcontainers(disabledWithoutDocker = true)
class CustomizedSynthesisDatabaseIntegrationTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("jobpuzzle_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void mariaDbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", MARIADB::getDriverClassName);
    }

    @Autowired private UserRepository userRepository;
    @Autowired private JobCategoryRepository jobCategoryRepository;
    @Autowired private AnalysisCaseRepository analysisCaseRepository;
    @Autowired private AnalysisInputSnapshotRepository snapshotRepository;
    @Autowired private PromptTemplateRepository promptTemplateRepository;
    @Autowired private AiCallLogRepository aiCallLogRepository;
    @Autowired private GuideContextResultRepository guideContextResultRepository;
    @Autowired private CustomizedSynthesisResultWriter resultWriter;
    @Autowired private ReadinessResultRepository readinessResultRepository;
    @Autowired private QuestionSetRepository questionSetRepository;

    @Test
    void persistsNormalLimitedResultInMariaDbWithoutQuestionSet() {
        Fixture fixture = fixture();
        resultWriter.write(fixture.snapshot().getSnapshotId(), fixture.runningLog().getAiCallLogId(), fixture.guideContext(),
                com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult.builder()
                        .readiness(com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult.Readiness.builder()
                                .status(ReadinessResultStatus.GUIDE_LACK).canGenerateQuestions(false)
                                .reason("guide unavailable").limitations(java.util.List.of("GUIDE_LACK")).build())
                        .requirementMatches(java.util.List.of()).questions(java.util.List.of()).tasks(java.util.List.of()).build());

        ReadinessResult stored = readinessResultRepository.findBySnapshot_SnapshotId(fixture.snapshot().getSnapshotId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ReadinessResultStatus.GUIDE_LACK);
        assertThat(stored.isCanGenerateQuestions()).isFalse();
        assertThat(questionSetRepository.existsBySnapshot_SnapshotIdAndInterviewMode(fixture.snapshot().getSnapshotId(),
                com.example.jobpuzzle.interview.entity.InterviewSessionMode.COMPANY_FIT)).isFalse();
        assertThat(aiCallLogRepository.findById(fixture.runningLog().getAiCallLogId()).orElseThrow().getStatus()).isEqualTo(AiCallLogStatus.SUCCEEDED);
        assertThat(analysisCaseRepository.findById(fixture.analysisCase().getAnalysisCaseId()).orElseThrow().getStatus()).isEqualTo(AnalysisCaseStatus.COMPLETED);
    }

    private Fixture fixture() {
        String suffix = java.util.UUID.randomUUID().toString();
        JobCategory category = jobCategoryRepository.save(JobCategory.builder().mainCategory("TEST").subCategory("BACKEND-" + suffix)
                .careerLevel(JobCategoryCareerLevel.NEW).build());
        User user = userRepository.save(User.createLocalUser("db-test-" + suffix, "encoded", suffix + "@example.test", "DB Test", category));
        AnalysisCase analysisCase = analysisCaseRepository.save(AnalysisCase.builder().user(user).jobCategory(category).build());
        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.ANALYZING);
        analysisCase = analysisCaseRepository.saveAndFlush(analysisCase);
        AnalysisInputSnapshot snapshot = snapshotRepository.saveAndFlush(AnalysisInputSnapshot.builder()
                .analysisCase(analysisCase).user(user).jobCategory(category).build());
        PromptTemplate prompt = promptTemplateRepository.saveAndFlush(PromptTemplate.builder().promptCode("PT-" + suffix).name("JSON-05")
                .version("v1").targetJson("JSON-05").templateText("{}").isActive(true).build());
        AiCallLog log = AiCallLog.pending(AiProvider.MOCK, "db-test", AiExecutionStage.CUSTOMIZED_SYNTHESIS,
                AiInputReferenceType.ANALYSIS_SNAPSHOT, String.valueOf(snapshot.getSnapshotId()), "fingerprint-" + suffix, prompt, null, null);
        log.start();
        log = aiCallLogRepository.saveAndFlush(log);
        GuideContextResult guideContext = guideContextResultRepository.saveAndFlush(
                GuideContextResult.create(user, String.valueOf(snapshot.getSnapshotId()), category, null, GuideMatchType.NONE));
        return new Fixture(analysisCase, snapshot, log, guideContext);
    }

    private record Fixture(AnalysisCase analysisCase, AnalysisInputSnapshot snapshot, AiCallLog runningLog,
                           GuideContextResult guideContext) { }
}
