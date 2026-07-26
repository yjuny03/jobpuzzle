package com.example.jobpuzzle.report.service;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogRepository;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiInputReferenceType;
import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluationMode;
import com.example.jobpuzzle.evaluation.repository.AnswerEvaluationRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.interview.entity.InterviewMessage;
import com.example.jobpuzzle.interview.entity.InterviewMessageSender;
import com.example.jobpuzzle.interview.entity.InterviewMessageType;
import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import com.example.jobpuzzle.interview.repository.InterviewMessageRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionRepository;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.report.entity.FinalReport;
import com.example.jobpuzzle.report.entity.ImprovementSuggestion;
import com.example.jobpuzzle.report.repository.FinalReportRepository;
import com.example.jobpuzzle.report.repository.ImprovementSuggestionRepository;
import com.example.jobpuzzle.report.entity.ImprovementSuggestionTargetType;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// JSON-07 Mock 응답 확인용 1회성 테스트. 실 로컬 MariaDB(dev DB)에 직접 데이터를 심고
// FinalReportService.generateFinalReport()를 호출해 결과를 확인한다. 확인 후 삭제 예정.
@SpringBootTest
class FinalReportServiceRealDataCheck {

    @Autowired private UserRepository userRepository;
    @Autowired private JobCategoryRepository jobCategoryRepository;
    @Autowired private PromptTemplateRepository promptTemplateRepository;
    @Autowired private InterviewSessionRepository interviewSessionRepository;
    @Autowired private InterviewSessionQuestionRepository interviewSessionQuestionRepository;
    @Autowired private InterviewMessageRepository interviewMessageRepository;
    @Autowired private AnswerEvaluationRepository answerEvaluationRepository;
    @Autowired private AiCallLogRepository aiCallLogRepository;
    @Autowired private FinalReportRepository finalReportRepository;
    @Autowired private ImprovementSuggestionRepository improvementSuggestionRepository;
    @Autowired private FinalReportService finalReportService;

    // JSON-07(FinalReportResult) 스키마 지시문. 실제 서비스에서는 DataInitializer나 관리자 화면에서
    // PromptTemplate row로 미리 심어둘 내용이라, 지금은 검증 테스트 안에서 같이 시딩한다.
    private static final String JSON07_TEMPLATE_TEXT = """
            면접 세션의 답변 평가 결과를 종합해 최종 리포트를 아래 JSON 형식으로만 작성한다.
            다른 설명 문장이나 코드블록(```) 없이, 이 구조를 따르는 순수 JSON 객체 하나로만 응답한다.

            {
              "overallScore": 0~100 사이 정수 (실제로 평가한 관점만 반영한 종합 기준 충족도),
              "scoreLabel": "세션 종합 기준 충족도",
              "categoryScores": {
                "intentMatch": 0~100 정수 또는 null,
                "specificity": 0~100 정수 또는 null,
                "ownRole": 0~100 정수 또는 null,
                "problemSolving": 0~100 정수 또는 null,
                "resultExpression": 0~100 정수 또는 null,
                "requirementConnection": 0~100 정수 또는 null (COMPANY_FIT 모드가 아니면 null),
                "guideAlignment": 0~100 정수 또는 null (적용 가이드가 없으면 null),
                "deliveryClarity": 0~100 정수 또는 null
              },
              "basisSummary": {
                "jobCategory": "직무 대분류/중분류 문자열",
                "careerLevel": "NEW" 또는 "EXPERIENCED" 또는 "ANY",
                "evaluationPassThreshold": 정수 (기준 점수),
                "usedGuide": {"guideId": 정수 또는 null, "version": "문자열 또는 null"},
                "requirementConnections": [{"requirement": "요구사항 문자열", "matchLevel": "HIGH"|"MEDIUM"|"LOW"|"NONE"|"INSUFFICIENT"}],
                "missingEvidence": ["부족한 근거 문자열", ...],
                "targetWeaknessTag": null,
                "targetDimension": null,
                "originEvaluationIds": []
              },
              "weaknessTagSummary": [
                {"tag": "INTENT_MATCH_WEAK"|"SPECIFICITY_WEAK"|"OWN_ROLE_WEAK"|"PROBLEM_SOLVING_WEAK"|"RESULT_EXPRESSION_WEAK"|"GUIDE_ALIGNMENT_WEAK"|"DELIVERY_CLARITY_WEAK", "count": 1 이상 정수}
              ],
              "nextPracticeRecommendation": [
                {"questionType": "GENERAL"|"COMPANY_FIT"|"EXPERIENCE"|"PROBLEM_SOLVING"|"SKILL", "reason": "추천 이유 문자열"}
              ],
              "improvementSuggestion": {
                "resume": ["보완 제안 문자열", ...],
                "coverLetter": ["보완 제안 문자열", ...],
                "portfolio": ["보완 제안 문자열", ...],
                "experienceNote": ["보완 제안 문자열", ...]
              },
              "learningDirection": ["학습 방향 문자열", ...]
            }

            BASIC 모드에서는 requirementConnection을 null로, weaknessTagSummary와 improvementSuggestion의 각 배열은 빈 배열로 채운다.
            """;

    private static final String JSON07_FORBIDDEN_RULES = """
            아래 [이번 세션 정보]/[평가성공한 답변별 결과]에 없는 내용을 임의로 지어내지 않는다.
            categoryScores·weaknessTagSummary·nextPracticeRecommendation의 값은 반드시 위에 명시된 목록 중에서만 선택한다.
            합격 가능성이나 채용 확률로 해석될 수 있는 표현은 쓰지 않는다.
            """;

    @Test
    void generatesFinalReportFromSeededCompanyFitSession() throws Exception {
        Setup setup = setup();
        String suffix = setup.suffix();

        InterviewSession session = InterviewSession.builder()
                .userId(setup.user().getUserId())
                .questionSetId(1L)
                .mode(InterviewSessionMode.COMPANY_FIT)
                .jobCategoryId(setup.category().getJobCategoryId())
                .build();
        session.complete();
        session = interviewSessionRepository.save(session);

        // Q1: 답변 제출 + 평가 성공
        InterviewSessionQuestion evaluatedQuestion = seedQuestion(session.getSessionId(), 1L, 1);
        InterviewMessage evaluatedAnswer = seedAnswer(evaluatedQuestion.getSessionQuestionId(), "원 질문에 대한 답변입니다.");
        seedEvaluation(evaluatedAnswer.getMessageId(), evaluatedQuestion.getSessionQuestionId(), suffix);

        // Q2: 답변은 제출됐지만 평가 결과 없음(평가 실패로 간주)
        InterviewSessionQuestion failedQuestion = seedQuestion(session.getSessionId(), 2L, 2);
        seedAnswer(failedQuestion.getSessionQuestionId(), "평가에 실패한 답변입니다.");

        // Q3: 답변 미제출(미답변/SKIPPED로 간주)
        seedQuestion(session.getSessionId(), 3L, 3);

        finalReportService.generateFinalReport(session.getSessionId());

        FinalReport report = finalReportRepository.findBySession_SessionId(session.getSessionId()).orElseThrow();
        assertThat(report.getTotalQuestionCount()).isEqualTo(3);
        assertThat(report.getSubmittedQuestionCount()).isEqualTo(2);
        assertThat(report.getEvaluatedQuestionCount()).isEqualTo(1);
        assertThat(report.getEvaluationFailedQuestionCount()).isEqualTo(1);
        assertThat(report.getSkippedQuestionCount()).isEqualTo(1);
        assertThat(report.getCompletionRate()).isEqualTo(67);
        assertThat(report.getOverallScore()).isEqualTo(78);
        assertThat(report.getCategoryScores()).isNotNull();
        assertThat(report.getBasisSummary()).isNotNull();
        // 픽스처 답변이 score=78(기준 70 통과)에 weaknessTags=[]라, 실제 Claude는 약점 태그를
        // 지어내지 않고 빈 배열을 줄 수 있다 - 그래서 개수 단정 대신 null이 아닌지만 확인한다.
        assertThat(report.getWeaknessTagSummary()).isNotNull();
        assertThat(report.getNextPracticeRecommendation()).isNotNull();
        assertThat(report.getLearningDirection()).isNotNull();

        // 픽스처 답변이 기준 통과+약점 없음이라 Claude가 보완 제안을 빈 배열로 줄 수 있다 -
        // 개수 단정하지 않고 조회 자체가 되는지만 확인한다.
        List<ImprovementSuggestion> suggestions = improvementSuggestionRepository
                .findByReportIdOrderByTargetTypeAscDisplayOrderAsc(report.getReportId());
        assertThat(suggestions).isNotNull();

        // report.getAiCallLog()는 지연 로딩 프록시라 세션 밖에서 필드 접근이 안 되므로 id로 다시 조회
        AiCallLog log = aiCallLogRepository.findById(report.getAiCallLog().getAiCallLogId()).orElseThrow();
        assertThat(log.getExecutionStage()).isEqualTo(AiExecutionStage.FINAL_REPORT);
        assertThat(log.getStatus()).isEqualTo(AiCallLogStatus.SUCCEEDED);

        printAsJson(report, suggestions);
    }

    // FR-REP-001: COMPLETED가 아닌 세션은 리포트를 생성하지 않는다.
    @Test
    void throwsWhenSessionNotCompleted() {
        Setup setup = setup();
        InterviewSession session = InterviewSession.builder()
                .userId(setup.user().getUserId())
                .questionSetId(1L)
                .mode(InterviewSessionMode.COMPANY_FIT)
                .jobCategoryId(setup.category().getJobCategoryId())
                .build(); // complete() 호출 안 함 → CREATED 상태 그대로
        session = interviewSessionRepository.save(session);
        Long sessionId = session.getSessionId();

        assertThatThrownBy(() -> finalReportService.generateFinalReport(sessionId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INTERVIEW_SESSION_NOT_COMPLETED);

        assertThat(finalReportRepository.existsBySession_SessionId(sessionId)).isFalse();
    }

    // FR-REP-001: 이미 리포트가 있는 세션은 다시 호출해도 새 리포트를 만들지 않는다(멱등).
    @Test
    void doesNotDuplicateWhenReportAlreadyExists() {
        Setup setup = setup();
        String suffix = setup.suffix();

        InterviewSession session = InterviewSession.builder()
                .userId(setup.user().getUserId())
                .questionSetId(1L)
                .mode(InterviewSessionMode.COMPANY_FIT)
                .jobCategoryId(setup.category().getJobCategoryId())
                .build();
        session.complete();
        session = interviewSessionRepository.save(session);
        Long sessionId = session.getSessionId();

        InterviewSessionQuestion question = seedQuestion(sessionId, 1L, 1);
        InterviewMessage answer = seedAnswer(question.getSessionQuestionId(), "답변입니다.");
        seedEvaluation(answer.getMessageId(), question.getSessionQuestionId(), suffix);

        finalReportService.generateFinalReport(sessionId);
        Long firstReportId = finalReportRepository.findBySession_SessionId(sessionId).orElseThrow().getReportId();

        finalReportService.generateFinalReport(sessionId);
        Long secondReportId = finalReportRepository.findBySession_SessionId(sessionId).orElseThrow().getReportId();

        assertThat(secondReportId).isEqualTo(firstReportId);
    }

    // FR-REP-001: 평가 성공(SUCCEEDED)이 하나도 없으면 리포트를 생성하지 않는다.
    @Test
    void doesNotGenerateWhenNoEvaluatedAnswer() {
        Setup setup = setup();

        InterviewSession session = InterviewSession.builder()
                .userId(setup.user().getUserId())
                .questionSetId(1L)
                .mode(InterviewSessionMode.COMPANY_FIT)
                .jobCategoryId(setup.category().getJobCategoryId())
                .build();
        session.complete();
        session = interviewSessionRepository.save(session);
        Long sessionId = session.getSessionId();

        // 답변은 제출됐지만 평가 결과가 없는 질문뿐 (평가 성공 0건)
        InterviewSessionQuestion question = seedQuestion(sessionId, 1L, 1);
        seedAnswer(question.getSessionQuestionId(), "평가 안 된 답변입니다.");

        assertThatThrownBy(() -> finalReportService.generateFinalReport(sessionId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FINAL_REPORT_NOT_GENERATABLE);

        assertThat(finalReportRepository.existsBySession_SessionId(sessionId)).isFalse();
    }

    private Setup setup() {
        String suffix = UUID.randomUUID().toString();

        JobCategory category = jobCategoryRepository.save(JobCategory.builder()
                .mainCategory("TEST").subCategory("REPORT-" + suffix).careerLevel(JobCategoryCareerLevel.NEW).build());
        User user = userRepository.save(User.createLocalUser(
                "report-check-" + suffix, "encoded", suffix + "@example.test", "리포트 확인용", category));

        promptTemplateRepository.save(PromptTemplate.builder()
                .promptCode("PT-JSON06-" + suffix).name("JSON-06 test").version("v1")
                .targetJson("JSON-06").templateText("{}").isActive(true).build());
        promptTemplateRepository.save(PromptTemplate.builder()
                .promptCode("PT-JSON07-" + suffix).name("JSON-07 test").version("v1")
                .targetJson("JSON-07")
                .templateText(JSON07_TEMPLATE_TEXT)
                .forbiddenRules(JSON07_FORBIDDEN_RULES)
                .isActive(true).build());

        return new Setup(suffix, user, category);
    }

    private record Setup(String suffix, User user, JobCategory category) {
    }

    // 문서(AI_파이프라인_JSON_구조_정의서 "예시 JSON" 시트)의 JSON-07 예시와 같은 필드 순서로
    // 실제 JSON 문자열을 찍어서, 눈으로 직접 대조할 수 있게 한다.
    private void printAsJson(FinalReport report, List<ImprovementSuggestion> suggestions) throws Exception {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("interviewMode", report.getInterviewMode());
        json.put("totalQuestionCount", report.getTotalQuestionCount());
        json.put("submittedQuestionCount", report.getSubmittedQuestionCount());
        json.put("evaluatedQuestionCount", report.getEvaluatedQuestionCount());
        json.put("evaluationFailedQuestionCount", report.getEvaluationFailedQuestionCount());
        json.put("skippedQuestionCount", report.getSkippedQuestionCount());
        json.put("completionRate", report.getCompletionRate());
        json.put("overallScore", report.getOverallScore());
        json.put("scoreLabel", report.getScoreLabel());
        json.put("categoryScores", report.getCategoryScores());
        json.put("basisSummary", report.getBasisSummary());
        json.put("weaknessTagSummary", report.getWeaknessTagSummary());
        json.put("nextPracticeRecommendation", report.getNextPracticeRecommendation());
        json.put("improvementSuggestion", groupSuggestions(suggestions));
        json.put("learningDirection", report.getLearningDirection());

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("=== FinalReport (JSON) ===");
        System.out.println(mapper.writeValueAsString(json));
    }

    // DB에는 targetType별 row로 나뉘어 저장돼 있어서, JSON-07 예시와 같은 모양으로 다시 묶어준다.
    private Map<String, List<String>> groupSuggestions(List<ImprovementSuggestion> suggestions) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        grouped.put("resume", textsOf(suggestions, ImprovementSuggestionTargetType.RESUME));
        grouped.put("coverLetter", textsOf(suggestions, ImprovementSuggestionTargetType.COVER_LETTER));
        grouped.put("portfolio", textsOf(suggestions, ImprovementSuggestionTargetType.PORTFOLIO));
        grouped.put("experienceNote", textsOf(suggestions, ImprovementSuggestionTargetType.EXPERIENCE_NOTE));
        return grouped;
    }

    private List<String> textsOf(List<ImprovementSuggestion> suggestions, ImprovementSuggestionTargetType targetType) {
        return suggestions.stream()
                .filter(s -> s.getTargetType() == targetType)
                .map(ImprovementSuggestion::getSuggestionText)
                .collect(Collectors.toList());
    }

    private InterviewSessionQuestion seedQuestion(Long sessionId, Long questionId, int displayOrder) {
        InterviewSessionQuestion question = InterviewSessionQuestion.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .questionTextSnapshot("질문 " + displayOrder)
                .intentSnapshot("의도 " + displayOrder)
                .evaluationFocusSnapshot("[]")
                .sourceRefsSnapshot("[]")
                .displayOrder(displayOrder)
                .build();
        return interviewSessionQuestionRepository.save(question);
    }

    private InterviewMessage seedAnswer(Long sessionQuestionId, String text) {
        InterviewMessage message = InterviewMessage.builder()
                .sessionQuestionId(sessionQuestionId)
                .sender(InterviewMessageSender.USER)
                .messageType(InterviewMessageType.ORIGINAL_ANSWER)
                .messageText(text)
                .build();
        message.confirm();
        return interviewMessageRepository.save(message);
    }

    private void seedEvaluation(Long answerMessageId, Long sessionQuestionId, String suffix) {
        PromptTemplate promptTemplate = promptTemplateRepository
                .findFirstByTargetJsonAndIsActiveTrueOrderByPromptTemplateIdDesc("JSON-06").orElseThrow();
        AiCallLog log = AiCallLog.pending(
                AiProvider.MOCK, "mock-model", AiExecutionStage.ANSWER_EVALUATION,
                AiInputReferenceType.ANSWER, String.valueOf(answerMessageId),
                "fingerprint-" + suffix, promptTemplate, null, null
        );
        log.start();
        log.succeed();
        log = aiCallLogRepository.save(log);

        AnswerEvaluation evaluation = AnswerEvaluation.builder()
                .answerMessageId(answerMessageId)
                .sessionQuestionId(sessionQuestionId)
                .evaluationMode(AnswerEvaluationMode.COMPANY_FIT)
                .score(78)
                .passThreshold(70)
                .scoreLabel("기준 충족")
                .evaluationDetail("{}")
                .weaknessTags("[]")
                .summary("평가 요약")
                .improvementDirection("[]")
                .aiCallLogId(log.getAiCallLogId())
                .build();
        answerEvaluationRepository.save(evaluation);
    }
}