package com.example.jobpuzzle.report.service;

import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.log.AiCallLogRepository;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.ai.service.AiClientService;
import com.example.jobpuzzle.evaluation.dto.SessionScoreSummary;
import com.example.jobpuzzle.evaluation.repository.AnswerEvaluationRepository;
import com.example.jobpuzzle.evaluation.service.SessionScoreAggregationService;
import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.QuestionSet;
import com.example.jobpuzzle.interview.repository.InterviewMessageRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionRepository;
import com.example.jobpuzzle.report.repository.FinalReportRepository;
import com.example.jobpuzzle.report.repository.ImprovementSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// FinalReportService.validateResult()가 JSON-07 응답의 최소 검증(overallScore 범위,
// WEAKNESS_REVIEW 모드 정합성, weaknessTagSummary count)을 제대로 걸러내는지 확인한다.
@ExtendWith(MockitoExtension.class)
class FinalReportServiceTest {

    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private InterviewSessionQuestionRepository interviewSessionQuestionRepository;
    @Mock private InterviewMessageRepository interviewMessageRepository;
    @Mock private AnswerEvaluationRepository answerEvaluationRepository;
    @Mock private SessionScoreAggregationService sessionScoreAggregationService;
    @Mock private FinalReportRepository finalReportRepository;
    @Mock private ImprovementSuggestionRepository improvementSuggestionRepository;
    @Mock private AiCallLogRepository aiCallLogRepository;
    @Mock private PromptTemplateRepository promptTemplateRepository;
    @Mock private AiClientService aiClientService;
    @Mock private PlatformTransactionManager transactionManager;

    private FinalReportService service;

    @BeforeEach
    void setUp() {
        service = new FinalReportService(
                interviewSessionRepository,
                interviewSessionQuestionRepository,
                interviewMessageRepository,
                answerEvaluationRepository,
                sessionScoreAggregationService,
                finalReportRepository,
                improvementSuggestionRepository,
                aiCallLogRepository,
                promptTemplateRepository,
                aiClientService,
                transactionManager,
                70
        );
    }

    @Test
    void passesWhenResultStaysWithinContract() {
        InterviewSession session = session(InterviewSessionMode.BASIC);
        SessionScoreSummary score = SessionScoreSummary.builder().overallScore(80).build();
        FinalReportResult result = FinalReportResult.builder()
                .overallScore(80)
                .weaknessTagSummary(List.of(
                        FinalReportResult.WeaknessTagSummary.builder().tag("specificity_weak").count(2).build()))
                .build();

        assertThatCode(() -> invokeValidateResult(result, session, score)).doesNotThrowAnyException();
    }

    @Test
    void rejectsOverallScoreOutOfRangeWhenAggregatedScoreIsMissing() {
        InterviewSession session = session(InterviewSessionMode.BASIC);
        SessionScoreSummary score = SessionScoreSummary.builder().overallScore(null).build();
        FinalReportResult result = FinalReportResult.builder()
                .overallScore(150)
                .build();

        assertThatThrownBy(() -> invokeValidateResult(result, session, score))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overallScore");
    }

    @Test
    void rejectsWeaknessFieldsOutsideWeaknessReviewMode() {
        InterviewSession session = session(InterviewSessionMode.COMPANY_FIT);
        SessionScoreSummary score = SessionScoreSummary.builder().overallScore(80).build();
        FinalReportResult result = FinalReportResult.builder()
                .overallScore(80)
                .basisSummary(FinalReportResult.BasisSummary.builder()
                        .targetWeaknessTag("specificity_weak")
                        .build())
                .build();

        assertThatThrownBy(() -> invokeValidateResult(result, session, score))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WEAKNESS_REVIEW");
    }

    @Test
    void rejectsNegativeWeaknessTagCount() {
        InterviewSession session = session(InterviewSessionMode.BASIC);
        SessionScoreSummary score = SessionScoreSummary.builder().overallScore(80).build();
        FinalReportResult result = FinalReportResult.builder()
                .overallScore(80)
                .weaknessTagSummary(List.of(
                        FinalReportResult.WeaknessTagSummary.builder().tag("specificity_weak").count(-1).build()))
                .build();

        assertThatThrownBy(() -> invokeValidateResult(result, session, score))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("weaknessTagSummary");
    }

    private void invokeValidateResult(FinalReportResult result, InterviewSession session, SessionScoreSummary score) {
        ReflectionTestUtils.invokeMethod(service, "validateResult", result, session, score);
    }

    private InterviewSession session(InterviewSessionMode mode) {
        QuestionSet questionSet = new QuestionSet();
        ReflectionTestUtils.setField(questionSet, "interviewMode", mode);
        return InterviewSession.create(questionSet);
    }
}