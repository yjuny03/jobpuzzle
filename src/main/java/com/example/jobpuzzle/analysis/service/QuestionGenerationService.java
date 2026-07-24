package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.log.*;
import com.example.jobpuzzle.ai.dto.InterviewQuestionGenerationResult;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRenderer;
import com.example.jobpuzzle.ai.service.AiClientService;
import com.example.jobpuzzle.ai.validation.AiResponseProcessor;
import com.example.jobpuzzle.ai.validation.InterviewQuestionGenerationResponseValidator;
import com.example.jobpuzzle.analysis.dto.BasicQuestionRequest;
import com.example.jobpuzzle.analysis.dto.QuestionSetResponse;
import com.example.jobpuzzle.analysis.dto.WeaknessQuestionRequest;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import com.example.jobpuzzle.evaluation.entity.WeaknessTagLog;
import com.example.jobpuzzle.evaluation.entity.WeaknessTagResolveStatus;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagLogRepository;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagStatusRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.interview.entity.*;
import com.example.jobpuzzle.interview.dto.QuestionHintResponse;
import com.example.jobpuzzle.interview.dto.QuestionListResponse;
import com.example.jobpuzzle.interview.repository.InterviewQuestionRepository;
import com.example.jobpuzzle.interview.repository.QuestionSetRepository;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionGenerationService {

    private final UserRepository userRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final QuestionSetRepository questionSetRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final WeaknessTagStatusRepository weaknessTagStatusRepository;
    private final WeaknessTagLogRepository weaknessTagLogRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final AiClientService aiClientService;
    private final PromptTemplateRenderer promptTemplateRenderer;
    private final AiResponseProcessor aiResponseProcessor;
    private final InterviewQuestionGenerationInputMapper inputMapper;
    private final InterviewQuestionGenerationResponseValidator responseValidator;
    private final InterviewQuestionGenerationResultWriter resultWriter;

    public QuestionSetResponse generateBasicQuestionSet(Long userId, BasicQuestionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        JobCategory jobCategory = jobCategoryRepository.findById(request.getJobCategoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND));
        if (jobCategory.getCareerLevel() != request.getCareerLevel()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        PromptTemplate template = activeTemplate("JSON-11");
        String prompt = promptTemplateRenderer.renderInterviewQuestionGeneration(
                template,
                "basicInputJson",
                inputMapper.basic(jobCategory)
        );
        AiCallLog log = startLog(
                template,
                AiExecutionStage.BASIC_QUESTION_GENERATION,
                AiInputReferenceType.JOB_CAREER_CRITERIA,
                jobCategory.getJobCategoryId() + ":" + request.getCareerLevel(),
                prompt
        );
        String rawResponse = aiClientService.generateBasicQuestions(prompt);
        InterviewQuestionGenerationResult generated =
                aiResponseProcessor.parseInterviewQuestions(rawResponse, "JSON-11");
        responseValidator.validateBasic(generated);

        QuestionSet set = questionSetRepository.save(QuestionSet.create(
                user,
                InterviewSessionMode.BASIC,
                jobCategory,
                request.getCareerLevel(),
                null,
                QuestionSetGenerationSource.AI,
                log.getPromptVersion(),
                null,
                null,
                List.of(),
                log
        ));

        List<InterviewQuestion> questions = resultWriter.saveBasic(set, generated);
        log.succeed();
        return QuestionSetResponse.from(set, questions);
    }

    public QuestionSetResponse generateWeaknessQuestionSet(
            Long userId,
            WeaknessQuestionRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        weaknessTagStatusRepository
                .findByUser_UserIdAndTag(userId, request.getTargetWeaknessTag())
                .filter(status -> status.getStatus() == WeaknessTagResolveStatus.UNRESOLVED)
                .orElseThrow(() -> new CustomException(ErrorCode.WEAKNESS_NOT_AVAILABLE));

        List<WeaknessTagLog> origins = weaknessTagLogRepository
                .findTop10ByUser_UserIdAndTagOrderByTagLogIdDesc(
                        userId,
                        request.getTargetWeaknessTag()
                );
        if (origins.isEmpty()) {
            throw new CustomException(ErrorCode.WEAKNESS_NOT_AVAILABLE);
        }

        AnswerEvaluation firstEvaluation = origins.get(0).getEvaluation();
        InterviewSession originSession = firstEvaluation.getSessionQuestion().getSession();
        String targetDimension = targetDimension(request.getTargetWeaknessTag());
        List<Long> basisEvaluationIds = origins.stream()
                .map(log -> log.getEvaluation().getEvaluationId())
                .distinct()
                .toList();

        PromptTemplate template = activeTemplate("JSON-09");
        String prompt = promptTemplateRenderer.renderInterviewQuestionGeneration(
                template,
                "weaknessInputJson",
                inputMapper.weakness(request.getTargetWeaknessTag(), targetDimension, origins)
        );
        AiCallLog log = startLog(
                template,
                AiExecutionStage.WEAKNESS_QUESTION_GENERATION,
                AiInputReferenceType.WEAKNESS_TAG,
                userId + ":" + request.getTargetWeaknessTag(),
                prompt
        );
        String rawResponse = aiClientService.generateWeaknessQuestions(prompt);
        InterviewQuestionGenerationResult generated =
                aiResponseProcessor.parseInterviewQuestions(rawResponse, "JSON-09");
        responseValidator.validateWeakness(
                generated,
                request.getTargetWeaknessTag(),
                targetDimension,
                basisEvaluationIds
        );

        QuestionSet set = questionSetRepository.save(QuestionSet.create(
                user,
                InterviewSessionMode.WEAKNESS_REVIEW,
                originSession.getJobCategory(),
                originSession.getCareerLevel(),
                originSession.getGuideContextResult(),
                QuestionSetGenerationSource.AI,
                log.getPromptVersion(),
                request.getTargetWeaknessTag(),
                targetDimension,
                basisEvaluationIds,
                log
        ));

        List<AnswerEvaluation> originEvaluations = origins.stream()
                .map(WeaknessTagLog::getEvaluation)
                .toList();
        List<InterviewQuestion> questions =
                resultWriter.saveWeakness(set, generated, originEvaluations);
        log.succeed();
        return QuestionSetResponse.from(set, questions);
    }

    @Transactional(readOnly = true)
    public QuestionSetResponse getQuestionSet(Long userId, Long questionSetId) {
        QuestionSet set = questionSetRepository
                .findByQuestionSetIdAndUser_UserId(questionSetId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_SET_NOT_READY));
        List<InterviewQuestion> questions =
                interviewQuestionRepository.findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(questionSetId);
        return QuestionSetResponse.from(set, questions);
    }

    @Transactional(readOnly = true)
    public List<QuestionListResponse> getQuestionList(Long userId, Long questionSetId) {
        questionSetRepository.findByQuestionSetIdAndUser_UserId(questionSetId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_SET_NOT_READY));
        return interviewQuestionRepository
                .findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(questionSetId)
                .stream()
                .filter(question -> question.getReviewStatus() == InterviewQuestionReviewStatus.PASS)
                .map(QuestionListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionHintResponse getQuestionHint(Long userId, Long questionId) {
        InterviewQuestion question = interviewQuestionRepository.findById(questionId)
                .filter(value -> value.getQuestionSet().getUser().getUserId().equals(userId))
                .filter(value -> value.getReviewStatus() == InterviewQuestionReviewStatus.PASS)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        return QuestionHintResponse.from(question);
    }

    private String targetDimension(String tag) {
        for (InterviewQuestionEvaluationFocus focus : InterviewQuestionEvaluationFocus.values()) {
            if (tag.startsWith(focus.name())) {
                return focus.name();
            }
        }
        return InterviewQuestionEvaluationFocus.specificity.name();
    }

    private AiCallLog startLog(
            PromptTemplate template,
            AiExecutionStage stage,
            AiInputReferenceType referenceType,
            String referenceId,
            String renderedPrompt
    ) {
        AiCallLog log = AiCallLog.pending(
                aiClientService.getProvider(),
                aiClientService.getModel(),
                stage,
                referenceType,
                referenceId,
                Integer.toHexString(renderedPrompt.hashCode()),
                template,
                null,
                null
        );
        aiCallLogRepository.save(log);
        log.start();
        return log;
    }

    private PromptTemplate activeTemplate(String targetJson) {
        return promptTemplateRepository
                .findFirstByTargetJsonAndIsActiveTrueOrderByPromptTemplateIdDesc(targetJson)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_PROMPT_TEMPLATE_NOT_FOUND));
    }
}
