package com.example.jobpuzzle.report.service;

import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.log.AiCallLogRepository;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.log.AiInputReferenceType;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.ai.service.AiClientService;
import com.example.jobpuzzle.ai.service.GenerationClientSelection;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import com.example.jobpuzzle.evaluation.dto.SessionScoreSummary;
import com.example.jobpuzzle.evaluation.repository.AnswerEvaluationRepository;
import com.example.jobpuzzle.evaluation.service.SessionScoreAggregationService;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.interview.entity.InterviewMessage;
import com.example.jobpuzzle.interview.entity.InterviewMessageType;
import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import com.example.jobpuzzle.interview.entity.InterviewSessionStatus;
import com.example.jobpuzzle.interview.repository.InterviewMessageRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionRepository;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.report.dto.FinalReportResponse;
import com.example.jobpuzzle.report.entity.FinalReport;
import com.example.jobpuzzle.report.entity.ImprovementSuggestion;
import com.example.jobpuzzle.report.entity.ImprovementSuggestionTargetType;
import com.example.jobpuzzle.report.repository.FinalReportRepository;
import com.example.jobpuzzle.report.repository.ImprovementSuggestionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinalReportService {

    private static final String TARGET_JSON = "JSON-07";

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewSessionQuestionRepository interviewSessionQuestionRepository;
    private final InterviewMessageRepository interviewMessageRepository;
    private final AnswerEvaluationRepository answerEvaluationRepository;
    private final SessionScoreAggregationService sessionScoreAggregationService;
    private final FinalReportRepository finalReportRepository;
    private final ImprovementSuggestionRepository improvementSuggestionRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AiClientService aiClientService;
    private final TransactionTemplate transactionTemplate;
    private final int evaluationPassThreshold;

    public FinalReportService(
            InterviewSessionRepository interviewSessionRepository,
            InterviewSessionQuestionRepository interviewSessionQuestionRepository,
            InterviewMessageRepository interviewMessageRepository,
            AnswerEvaluationRepository answerEvaluationRepository,
            SessionScoreAggregationService sessionScoreAggregationService,
            FinalReportRepository finalReportRepository,
            ImprovementSuggestionRepository improvementSuggestionRepository,
            AiCallLogRepository aiCallLogRepository,
            PromptTemplateRepository promptTemplateRepository,
            AiClientService aiClientService,
            PlatformTransactionManager transactionManager,
            @Value("${evaluation.pass-threshold}") int evaluationPassThreshold
    ) {
        this.interviewSessionRepository = interviewSessionRepository;
        this.interviewSessionQuestionRepository = interviewSessionQuestionRepository;
        this.interviewMessageRepository = interviewMessageRepository;
        this.answerEvaluationRepository = answerEvaluationRepository;
        this.sessionScoreAggregationService = sessionScoreAggregationService;
        this.finalReportRepository = finalReportRepository;
        this.improvementSuggestionRepository = improvementSuggestionRepository;
        this.aiCallLogRepository = aiCallLogRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.aiClientService = aiClientService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.evaluationPassThreshold = evaluationPassThreshold;
    }

    // JSON-07: DB 선점과 AI 호출을 분리한 짧은 트랜잭션 구조는 analysis/service/InitialAnalysisStageExecutor와
    // 같은 짜임을 답변 평가 대신 세션 단위로 가져온 것이다.
    public void generateFinalReport(Long sessionId) {
        ReportLease lease = acquire(sessionId);
        if (lease == null) {
            return;
        }

        try {
            String prompt = buildPrompt(lease);
            FinalReportResult result = aiClientService.finalReport(lease.selection(), prompt);
            saveSuccess(lease, result);
        } catch (RuntimeException exception) {
            recordFailure(lease.aiCallLogId(), exception);
        }
    }

    // 저장된 리포트가 있으면 그대로 반환
    // 없으면 생성 가능 조건을 확인한 뒤 그 자리에서 생성해서 반환
    public FinalReportResponse getFinalReport(Long userId, Long sessionId) {
        InterviewSession session = interviewSessionRepository.findBySessionIdAndUser_UserIdAndDeletedAtIsNull(sessionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        Optional<FinalReport> report = finalReportRepository.findBySession_SessionId(sessionId);
        if (report.isEmpty()) {
            generateIfPossible(session);
            report = finalReportRepository.findBySession_SessionId(sessionId);
            if (report.isEmpty()) {
                throw new CustomException(ErrorCode.FINAL_REPORT_GENERATION_FAILED);
            }
        }
        return toResponse(report.get());
    }

    private void generateIfPossible(InterviewSession session) {
        if (session.getStatus() != InterviewSessionStatus.COMPLETED) {
            throw new CustomException(ErrorCode.INTERVIEW_SESSION_NOT_COMPLETED);
        }

        Optional<AiCallLog> latest = aiCallLogRepository
                .findFirstByExecutionStageAndInputReferenceTypeAndInputReferenceIdOrderByAiCallLogIdDesc(
                        AiExecutionStage.FINAL_REPORT, AiInputReferenceType.INTERVIEW_SESSION,
                        String.valueOf(session.getSessionId()));
        if (latest.isPresent()) {
            AiCallLogStatus latestStatus = latest.get().getStatus();
            if (latestStatus == AiCallLogStatus.PENDING || latestStatus == AiCallLogStatus.RUNNING) {
                throw new CustomException(ErrorCode.FINAL_REPORT_GENERATION_IN_PROGRESS);
            }
        }

        generateFinalReport(session.getSessionId());
    }

    private FinalReportResponse toResponse(FinalReport report) {
        List<ImprovementSuggestion> suggestions = improvementSuggestionRepository
                .findByReportIdOrderByTargetTypeAscDisplayOrderAsc(report.getReportId());

        return FinalReportResponse.builder()
                .sessionId(report.getSession().getSessionId())
                .analysisCaseId(analysisCaseIdOf(report.getSession()))
                .interviewMode(report.getInterviewMode().name())
                .totalQuestionCount(report.getTotalQuestionCount())
                .submittedQuestionCount(report.getSubmittedQuestionCount())
                .evaluatedQuestionCount(report.getEvaluatedQuestionCount())
                .evaluationFailedQuestionCount(report.getEvaluationFailedQuestionCount())
                .skippedQuestionCount(report.getSkippedQuestionCount())
                .completionRate(report.getCompletionRate())
                .overallScore(report.getOverallScore())
                .scoreLabel(report.getScoreLabel())
                .overallAssessment(report.getOverallAssessment())
                .categoryScores(toResponseCategoryScores(report.getCategoryScores()))
                .categoryScoreReasons(toResponseCategoryScoreReasons(report.getCategoryScoreReasons()))
                .basisSummary(toResponseBasisSummary(report.getBasisSummary()))
                .weaknessTagSummary(toResponseWeaknessTagSummaries(report.getWeaknessTagSummary()))
                .nextPracticeRecommendation(toResponseNextPracticeRecommendations(report.getNextPracticeRecommendation()))
                .improvementSuggestion(toResponseImprovementSuggestion(suggestions))
                .learningDirection(report.getLearningDirection())
                .build();
    }

    // 분석 결과 화면 링크용
    private Long analysisCaseIdOf(InterviewSession session) {
        return session.getSnapshot() == null || session.getSnapshot().getAnalysisCase() == null
                ? null
                : session.getSnapshot().getAnalysisCase().getAnalysisCaseId();
    }

    private FinalReportResponse.CategoryScores toResponseCategoryScores(
            FinalReport.CategoryScores source
    ) {
        if (source == null) {
            return null;
        }
        return FinalReportResponse.CategoryScores.builder()
                .intentMatch(source.getIntentMatch())
                .specificity(source.getSpecificity())
                .ownRole(source.getOwnRole())
                .problemSolving(source.getProblemSolving())
                .resultExpression(source.getResultExpression())
                .requirementConnection(source.getRequirementConnection())
                .guideAlignment(source.getGuideAlignment())
                .deliveryClarity(source.getDeliveryClarity())
                .build();
    }

    private FinalReportResponse.CategoryScoreReasons toResponseCategoryScoreReasons(
            FinalReport.CategoryScoreReasons source
    ) {
        if (source == null) {
            return null;
        }
        return FinalReportResponse.CategoryScoreReasons.builder()
                .intentMatch(source.getIntentMatch())
                .specificity(source.getSpecificity())
                .ownRole(source.getOwnRole())
                .problemSolving(source.getProblemSolving())
                .resultExpression(source.getResultExpression())
                .requirementConnection(source.getRequirementConnection())
                .guideAlignment(source.getGuideAlignment())
                .deliveryClarity(source.getDeliveryClarity())
                .build();
    }

    private FinalReportResponse.BasisSummary toResponseBasisSummary(
            FinalReport.BasisSummary source
    ) {
        if (source == null) {
            return null;
        }
        return FinalReportResponse.BasisSummary.builder()
                .jobCategory(source.getJobCategory())
                .careerLevel(source.getCareerLevel() == null ? null : source.getCareerLevel().name())
                .evaluationPassThreshold(source.getEvaluationPassThreshold())
                .usedGuide(source.getUsedGuide() == null ? null
                        : FinalReportResponse.UsedGuide.builder()
                        .guideId(source.getUsedGuide().getGuideId())
                        .version(source.getUsedGuide().getVersion())
                        .build())
                .requirementConnections(source.getRequirementConnections() == null ? null
                        : source.getRequirementConnections().stream()
                        .map(connection -> FinalReportResponse.RequirementConnection.builder()
                                .requirement(connection.getRequirement())
                                .matchLevel(connection.getMatchLevel() == null ? null : connection.getMatchLevel().name())
                                .build())
                        .toList())
                .missingEvidence(source.getMissingEvidence())
                .targetWeaknessTag(source.getTargetWeaknessTag())
                .targetDimension(source.getTargetDimension())
                .originEvaluationIds(source.getOriginEvaluationIds())
                .build();
    }

    private List<FinalReportResponse.WeaknessTagSummary> toResponseWeaknessTagSummaries(
            List<FinalReport.WeaknessTagSummary> source
    ) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(tag -> FinalReportResponse.WeaknessTagSummary.builder()
                        .tag(tag.getTag())
                        .count(tag.getCount())
                        .reason(tag.getReason())
                        .build())
                .toList();
    }

    private List<FinalReportResponse.NextPracticeRecommendation> toResponseNextPracticeRecommendations(
            List<FinalReport.NextPracticeRecommendation> source
    ) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(recommendation -> FinalReportResponse.NextPracticeRecommendation.builder()
                        .questionType(recommendation.getQuestionType() == null ? null : recommendation.getQuestionType().name())
                        .reason(recommendation.getReason())
                        .build())
                .toList();
    }

    private FinalReportResponse.ImprovementSuggestion toResponseImprovementSuggestion(
            List<ImprovementSuggestion> suggestions
    ) {
        return FinalReportResponse.ImprovementSuggestion.builder()
                .resume(textsOf(suggestions, ImprovementSuggestionTargetType.RESUME))
                .coverLetter(textsOf(suggestions, ImprovementSuggestionTargetType.COVER_LETTER))
                .portfolio(textsOf(suggestions, ImprovementSuggestionTargetType.PORTFOLIO))
                .experienceNote(textsOf(suggestions, ImprovementSuggestionTargetType.EXPERIENCE_NOTE))
                .build();
    }

    private List<String> textsOf(List<ImprovementSuggestion> suggestions, ImprovementSuggestionTargetType targetType) {
        return suggestions.stream()
                .filter(suggestion -> suggestion.getTargetType() == targetType)
                .map(ImprovementSuggestion::getSuggestionText)
                .toList();
    }

    public void getReadinessScore() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void getEvidenceSummary() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void recommendNextPractice() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void getWeaknessTagGroups() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    // 세션 상태·기존 리포트·진행 중인 로그를 확인하고 새 RUNNING 로그를 선점한다.
    private ReportLease acquire(Long sessionId) {
        return transactionTemplate.execute(status -> {
            InterviewSession session = findSession(sessionId);
            if (session.getStatus() != InterviewSessionStatus.COMPLETED) {
                throw new CustomException(ErrorCode.INTERVIEW_SESSION_NOT_COMPLETED);
            }
            if (finalReportRepository.existsBySession_SessionId(sessionId)) {
                return null;
            }

            QuestionAggregate aggregate = aggregate(sessionId);
            if (aggregate.evaluatedQuestionCount() == 0) {
                throw new CustomException(ErrorCode.FINAL_REPORT_NOT_GENERATABLE);
            }
            SessionScoreSummary scoreSummary =
                    sessionScoreAggregationService.aggregate(session.getUser().getUserId(), sessionId);

            PromptTemplate promptTemplate = findActivePromptTemplate();
            GenerationClientSelection selection =
                    aiClientService.resolve(AiExecutionStage.FINAL_REPORT);
            String inputReferenceId = String.valueOf(sessionId);

            Optional<AiCallLog> latest = aiCallLogRepository
                    .findFirstByExecutionStageAndInputReferenceTypeAndInputReferenceIdOrderByAiCallLogIdDesc(
                            AiExecutionStage.FINAL_REPORT, AiInputReferenceType.INTERVIEW_SESSION, inputReferenceId
                    );
            if (latest.isPresent()) {
                AiCallLogStatus latestStatus = latest.get().getStatus();
                if (latestStatus == AiCallLogStatus.PENDING || latestStatus == AiCallLogStatus.RUNNING) {
                    return null;
                }
                if (latestStatus == AiCallLogStatus.SUCCEEDED) {
                    // 리포트 존재 여부는 위에서 이미 확인했으므로, SUCCEEDED인데 여기 도달했다면 데이터 오류
                    throw new IllegalStateException(
                            "FINAL_REPORT log is SUCCEEDED but no FinalReport row exists for session " + sessionId);
                }
            }

            AiCallLog log = AiCallLog.pending(
                    selection.provider(),
                    selection.model(),
                    AiExecutionStage.FINAL_REPORT,
                    AiInputReferenceType.INTERVIEW_SESSION,
                    inputReferenceId,
                    fingerprint(sessionId, promptTemplate),
                    promptTemplate,
                    null,
                    latest.filter(l -> l.getStatus() == AiCallLogStatus.FAILED).orElse(null)
            );
            log.start();
            aiCallLogRepository.saveAndFlush(log);

            return new ReportLease(
                    sessionId, log.getAiCallLogId(), session, aggregate, scoreSummary, selection, promptTemplate);
        });
    }

    // 세션의 질문별 진행 상태를 훑어 FR-REP-003 카운트와 프롬프트에 쓸 평가 목록을 계산한다.
    private QuestionAggregate aggregate(Long sessionId) {
        List<InterviewSessionQuestion> questions = interviewSessionQuestionRepository
                .findBySessionIdOrderByDisplayOrderAsc(sessionId);

        int submitted = 0;
        int evaluated = 0;
        int evaluationFailed = 0;
        List<AnswerEvaluation> evaluations = new ArrayList<>();

        for (InterviewSessionQuestion question : questions) {
            Optional<InterviewMessage> answer = interviewMessageRepository
                    .findBySessionQuestionIdAndMessageType(question.getSessionQuestionId(), InterviewMessageType.ORIGINAL_ANSWER);
            if (answer.isEmpty()) {
                continue;
            }
            submitted++;

            Optional<AnswerEvaluation> evaluation = answerEvaluationRepository
                    .findByAnswerMessageId(answer.get().getMessageId());
            if (evaluation.isPresent()) {
                evaluated++;
                evaluations.add(evaluation.get());
            } else {
                evaluationFailed++;
            }
        }

        int total = questions.size();
        int skipped = total - submitted;
        int completionRate = total == 0 ? 0 : (int) Math.round(submitted * 100.0 / total);

        return new QuestionAggregate(total, submitted, evaluated, evaluationFailed, skipped, completionRate, evaluations);
    }

    private PromptTemplate findActivePromptTemplate() {
        return promptTemplateRepository
                .findFirstByTargetJsonAndIsActiveTrueOrderByPromptTemplateIdDesc(TARGET_JSON)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_PROMPT_TEMPLATE_NOT_FOUND));
    }

    private String buildPrompt(ReportLease lease) {
        QuestionAggregate aggregate = lease.aggregate();
        SessionScoreSummary score = lease.scoreSummary();
        PromptTemplate promptTemplate = lease.promptTemplate();
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptTemplate.getTemplateText());
        if (promptTemplate.getForbiddenRules() != null && !promptTemplate.getForbiddenRules().isBlank()) {
            prompt.append("\n\n[금지 규칙]\n").append(promptTemplate.getForbiddenRules());
        }
        prompt.append("\n\n[이번 세션 정보]\n");
        prompt.append("세션 ").append(lease.sessionId())
                .append(" 모드=").append(lease.session().getMode())
                .append(" 제출자료=").append(submittedDocumentTypes(lease.session()))
                .append(" 총질문=").append(score.getTotalQuestionCount())
                .append(" 제출=").append(score.getSubmittedQuestionCount())
                .append(" 평가성공=").append(score.getEvaluatedQuestionCount())
                .append(" 평가실패=").append(score.getEvaluationFailedQuestionCount())
                .append(" 미답변=").append(score.getSkippedQuestionCount())
                .append(" overallScore=").append(score.getOverallScore())
                .append(" categoryScores=").append(score.getCategoryScores())
                .append('\n');
        for (AnswerEvaluation evaluation : aggregate.evaluations()) {
            prompt.append("- score=").append(evaluation.getScore())
                    .append(" weaknessTags=").append(evaluation.getWeaknessTags())
                    .append(" summary=").append(evaluation.getSummary())
                    .append(" evaluationDetail=").append(evaluationDetailText(evaluation.getEvaluationDetail()))
                    .append('\n');
        }
        return prompt.toString();
    }

    // categoryScoreReasons가 실제 근거(관점별 comment)를 인용할 수 있도록 evaluationDetail을 읽을 수 있게 펼친다.
    private String evaluationDetailText(Map<String, AnswerEvaluation.DimensionEvaluation> detail) {
        if (detail == null || detail.isEmpty()) {
            return "{}";
        }
        return detail.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getScore() != null)
                .map(entry -> entry.getKey() + "=" + entry.getValue().getScore()
                        + "(" + entry.getValue().getComment() + ")")
                .collect(Collectors.joining(", ", "{", "}"));
    }

    // BASIC 세션은 candidateAnalysis가 없어 제출자료를 알 수 없으므로 빈 목록으로 취급한다.
    private List<UserDocumentType> submittedDocumentTypes(InterviewSession session) {
        return session.getCandidateAnalysis() == null
                ? List.of()
                : session.getCandidateAnalysis().getAvailableDocumentTypes();
    }

    // AI 응답을 FinalReport(+ COMPANY_FIT이면 ImprovementSuggestion)로 저장하고 로그를 SUCCEEDED로 종결한다.
    private void saveSuccess(ReportLease lease, FinalReportResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            AiCallLog log = findLog(lease.aiCallLogId());
            if (finalReportRepository.existsBySession_SessionId(lease.sessionId())) {
                log.succeed();
                return;
            }

            InterviewSession session = findSession(lease.sessionId());
            SessionScoreSummary score = lease.scoreSummary();
            validateResult(result, session, score);

            FinalReport report = FinalReport.builder()
                    .session(session)
                    .interviewMode(session.getMode())
                    .totalQuestionCount(score.getTotalQuestionCount())
                    .submittedQuestionCount(score.getSubmittedQuestionCount())
                    .evaluatedQuestionCount(score.getEvaluatedQuestionCount())
                    .evaluationFailedQuestionCount(score.getEvaluationFailedQuestionCount())
                    .skippedQuestionCount(score.getSkippedQuestionCount())
                    .completionRate(score.getCompletionRate())
                    .overallScore(score.getOverallScore() == null
                            ? result.getOverallScore()
                            : score.getOverallScore())
                    .scoreLabel(result.getScoreLabel())
                    .overallAssessment(result.getOverallAssessment() == null || result.getOverallAssessment().isBlank()
                            ? "이번 세션의 총평을 생성하지 못했습니다."
                            : result.getOverallAssessment())
                    .categoryScores(toCategoryScores(score.getCategoryScores()))
                    .categoryScoreReasons(toCategoryScoreReasons(result.getCategoryScoreReasons()))
                    .basisSummary(toBasisSummary(result.getBasisSummary()))
                    .weaknessTagSummary(toWeaknessTagSummaries(result.getWeaknessTagSummary()))
                    .nextPracticeRecommendation(toNextPracticeRecommendations(result.getNextPracticeRecommendation()))
                    .learningDirection(result.getLearningDirection())
                    .aiCallLog(log)
                    .build();
            finalReportRepository.saveAndFlush(report);

            if (session.getMode() == InterviewSessionMode.COMPANY_FIT && result.getImprovementSuggestion() != null) {
                saveImprovementSuggestions(
                        report.getReportId(), result.getImprovementSuggestion(), submittedDocumentTypes(session));
            }

            log.succeed();
        });
    }

    // LLM 응답 중 화면에 그대로 노출되거나 세션 모드와 어긋나면 문제가 되는 필드만 최소로 검증한다.
    private void validateResult(FinalReportResult result, InterviewSession session, SessionScoreSummary score) {
        if (score.getOverallScore() == null && result.getOverallScore() != null
                && (result.getOverallScore() < 0 || result.getOverallScore() > 100)) {
            throw new IllegalStateException("JSON-07 overallScore out of range: " + result.getOverallScore());
        }

        FinalReportResult.BasisSummary basisSummary = result.getBasisSummary();
        if (session.getMode() != InterviewSessionMode.WEAKNESS_REVIEW && basisSummary != null
                && (basisSummary.getTargetWeaknessTag() != null || basisSummary.getTargetDimension() != null)) {
            throw new IllegalStateException(
                    "JSON-07 targetWeaknessTag/targetDimension must be null outside WEAKNESS_REVIEW mode");
        }

        if (result.getWeaknessTagSummary() != null
                && result.getWeaknessTagSummary().stream().anyMatch(tag -> tag.getCount() < 0)) {
            throw new IllegalStateException("JSON-07 weaknessTagSummary count must not be negative");
        }
    }

    private void saveImprovementSuggestions(
            Long reportId, FinalReportResult.ImprovementSuggestion suggestion, List<UserDocumentType> submittedTypes
    ) {
        List<ImprovementSuggestion> rows = new ArrayList<>();
        rows.addAll(toSuggestionRows(reportId, ImprovementSuggestionTargetType.RESUME, suggestion.getResume(), submittedTypes));
        rows.addAll(toSuggestionRows(reportId, ImprovementSuggestionTargetType.COVER_LETTER, suggestion.getCoverLetter(), submittedTypes));
        rows.addAll(toSuggestionRows(reportId, ImprovementSuggestionTargetType.PORTFOLIO, suggestion.getPortfolio(), submittedTypes));
        rows.addAll(toSuggestionRows(reportId, ImprovementSuggestionTargetType.EXPERIENCE_NOTE, suggestion.getExperienceNote(), submittedTypes));
        improvementSuggestionRepository.saveAll(rows);
    }

    // AI가 프롬프트 지시를 어기고 제출하지 않은 자료에 보완 제안을 만들어도 여기서 걸러낸다.
    private List<ImprovementSuggestion> toSuggestionRows(
            Long reportId, ImprovementSuggestionTargetType targetType, List<String> texts,
            List<UserDocumentType> submittedTypes
    ) {
        if (texts == null || !submittedTypes.contains(UserDocumentType.valueOf(targetType.name()))) {
            return List.of();
        }
        List<ImprovementSuggestion> rows = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            rows.add(ImprovementSuggestion.builder()
                    .reportId(reportId)
                    .targetType(targetType)
                    .suggestionText(texts.get(i))
                    .displayOrder(i)
                    .build());
        }
        return rows;
    }

    private void recordFailure(Long aiCallLogId, RuntimeException exception) {
        transactionTemplate.executeWithoutResult(status -> {
            AiCallLog log = findLog(aiCallLogId);
            if (log.getStatus() == AiCallLogStatus.RUNNING) {
                log.fail(AiCallLogErrorType.PROVIDER_ERROR, summarize(exception));
            }
        });
    }

    private InterviewSession findSession(Long sessionId) {
        return interviewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));
    }

    private AiCallLog findLog(Long aiCallLogId) {
        return aiCallLogRepository.findById(aiCallLogId)
                .orElseThrow(() -> new IllegalStateException("AI call log not found: " + aiCallLogId));
    }

    private String summarize(RuntimeException exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private String fingerprint(Long sessionId, PromptTemplate promptTemplate) {
        String value = AiExecutionStage.FINAL_REPORT + "|" + sessionId + "|"
                + promptTemplate.getPromptTemplateId() + "|" + promptTemplate.getVersion();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private FinalReport.CategoryScores toCategoryScores(FinalReportResult.CategoryScores source) {
        if (source == null) {
            return null;
        }
        return FinalReport.CategoryScores.builder()
                .intentMatch(source.getIntentMatch())
                .specificity(source.getSpecificity())
                .ownRole(source.getOwnRole())
                .problemSolving(source.getProblemSolving())
                .resultExpression(source.getResultExpression())
                .requirementConnection(source.getRequirementConnection())
                .guideAlignment(source.getGuideAlignment())
                .deliveryClarity(source.getDeliveryClarity())
                .build();
    }

    private FinalReport.CategoryScores toCategoryScores(
            Map<String, Integer> source
    ) {
        if (source == null) {
            return FinalReport.CategoryScores.builder().build();
        }
        return FinalReport.CategoryScores.builder()
                .intentMatch(source.get("intentMatch"))
                .specificity(source.get("specificity"))
                .ownRole(source.get("ownRole"))
                .problemSolving(source.get("problemSolving"))
                .resultExpression(source.get("resultExpression"))
                .requirementConnection(source.get("requirementConnection"))
                .guideAlignment(source.get("guideAlignment"))
                .deliveryClarity(source.get("deliveryClarity"))
                .build();
    }

    private FinalReport.CategoryScoreReasons toCategoryScoreReasons(FinalReportResult.CategoryScoreReasons source) {
        if (source == null) {
            return null;
        }
        return FinalReport.CategoryScoreReasons.builder()
                .intentMatch(source.getIntentMatch())
                .specificity(source.getSpecificity())
                .ownRole(source.getOwnRole())
                .problemSolving(source.getProblemSolving())
                .resultExpression(source.getResultExpression())
                .requirementConnection(source.getRequirementConnection())
                .guideAlignment(source.getGuideAlignment())
                .deliveryClarity(source.getDeliveryClarity())
                .build();
    }

    private FinalReport.BasisSummary toBasisSummary(FinalReportResult.BasisSummary source) {
        if (source == null) {
            return null;
        }
        return FinalReport.BasisSummary.builder()
                .jobCategory(source.getJobCategory())
                .careerLevel(source.getCareerLevel())
                .evaluationPassThreshold(source.getEvaluationPassThreshold())
                .usedGuide(source.getUsedGuide() == null ? null : FinalReport.UsedGuide.builder()
                        .guideId(source.getUsedGuide().getGuideId())
                        .version(source.getUsedGuide().getVersion())
                        .build())
                .requirementConnections(source.getRequirementConnections() == null ? null
                        : source.getRequirementConnections().stream()
                        .map(connection -> FinalReport.RequirementConnections.builder()
                                .requirement(connection.getRequirement())
                                .matchLevel(connection.getMatchLevel())
                                .build())
                        .toList())
                .missingEvidence(source.getMissingEvidence())
                .targetWeaknessTag(source.getTargetWeaknessTag())
                .targetDimension(source.getTargetDimension())
                .originEvaluationIds(source.getOriginEvaluationIds())
                .build();
    }

    private List<FinalReport.WeaknessTagSummary> toWeaknessTagSummaries(List<FinalReportResult.WeaknessTagSummary> source) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(tag -> FinalReport.WeaknessTagSummary.builder()
                        .tag(tag.getTag())
                        .count(tag.getCount())
                        .reason(tag.getReason())
                        .build())
                .toList();
    }

    private List<FinalReport.NextPracticeRecommendation> toNextPracticeRecommendations(
            List<FinalReportResult.NextPracticeRecommendation> source
    ) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(recommendation -> FinalReport.NextPracticeRecommendation.builder()
                        .questionType(recommendation.getQuestionType())
                        .reason(recommendation.getReason())
                        .build())
                .toList();
    }

    private record QuestionAggregate(
            int totalQuestionCount,
            int submittedQuestionCount,
            int evaluatedQuestionCount,
            int evaluationFailedQuestionCount,
            int skippedQuestionCount,
            int completionRate,
            List<AnswerEvaluation> evaluations
    ) {
    }

    private record ReportLease(
            Long sessionId,
            Long aiCallLogId,
            InterviewSession session,
            QuestionAggregate aggregate,
            SessionScoreSummary scoreSummary,
            GenerationClientSelection selection,
            PromptTemplate promptTemplate
    ) {
    }
}
