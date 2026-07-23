package com.example.jobpuzzle.ai.validation;

import com.example.jobpuzzle.ai.client.MockAiClient;
import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRenderer;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.analysis.entity.ReadinessResultStatus;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.guide.dto.GuideContextResultDto;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomizedAnalysisResponseValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiResponseProcessor processor = new AiResponseProcessor(objectMapper, new AnalysisSourceMarkerParser());
    private final CustomizedAnalysisResponseValidator validator = new CustomizedAnalysisResponseValidator();
    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer(objectMapper);
    private final MockAiClient mock = new MockAiClient(objectMapper, new AnalysisSourceMarkerParser());

    @Test
    void rendersMockParsesAndValidatesFullCustomizedAnalysisWithoutPersistence() {
        JobPostingAnalysisResult posting = posting();
        CandidateMaterialAnalysisResult candidate = candidate();
        GuideContextResultDto guide = guide(GuideMatchType.EXACT);
        String prompt = renderer.renderCustomizedAnalysis(template(), "개발", "백엔드", "EXPERIENCED", posting, candidate, guide);

        String raw = mock.generateCustomizedAnalysis(prompt);
        CustomizedAnalysisGenerationResult parsed = processor.parseCustomizedAnalysis("설명문\n" + raw + "\n끝");
        CustomizedAnalysisGenerationResult validated = validator.validate(new CustomizedAnalysisValidationContext(posting, candidate, guide), parsed);

        assertThat(prompt).contains("<JOB_POSTING_ANALYSIS>", "\"requirementId\":\"req-1\"");
        assertThat(validated.getReadiness().getStatus()).isEqualTo(ReadinessResultStatus.SUFFICIENT);
        assertThat(validated.getRequirementMatches()).singleElement().extracting(CustomizedAnalysisGenerationResult.RequirementMatch::getRequirementId).isEqualTo("req-1");
        assertThat(validated.getQuestions()).singleElement().extracting(CustomizedAnalysisGenerationResult.Question::getReviewStatus)
                .isEqualTo(com.example.jobpuzzle.interview.entity.InterviewQuestionReviewStatus.PASS);
    }

    @Test
    void rejectsUnknownAndMultipleJsonObjectsDuringStrictParsing() {
        assertFailure(() -> processor.parseCustomizedAnalysis("{\"readiness\":null,\"requirementMatches\":[],\"questions\":[],\"tasks\":[],\"extra\":true}"), AiCallLogErrorType.RESPONSE_PARSE_FAILED);
        assertFailure(() -> processor.parseCustomizedAnalysis("{} {}"), AiCallLogErrorType.RESPONSE_PARSE_FAILED);
    }

    @Test
    void validatesReadinessPriorityAndQuestionRules() {
        JobPostingAnalysisResult posting = posting();
        CandidateMaterialAnalysisResult noCandidate = CandidateMaterialAnalysisResult.builder()
                .availableDocumentTypes(List.of(UserDocumentType.RESUME))
                .resume(CandidateMaterialAnalysisResult.Resume.builder().experiences(List.of()).skills(List.of()).roles(List.of()).results(List.of()).build())
                .missingEvidence(List.of()).build();
        CustomizedAnalysisGenerationResult invalid = CustomizedAnalysisGenerationResult.builder()
                .readiness(CustomizedAnalysisGenerationResult.Readiness.builder().status(ReadinessResultStatus.SUFFICIENT)
                        .canGenerateQuestions(true).reason("잘못된 상태").limitations(List.of()).build())
                .requirementMatches(List.of(match(MatchAnalysisResultMatchLevel.NONE, List.of())))
                .questions(List.of()).tasks(List.of(task("match-1", MatchAnalysisResultMatchLevel.NONE))).build();

        assertFailure(() -> validator.validate(new CustomizedAnalysisValidationContext(posting, noCandidate, guide(GuideMatchType.NONE)), invalid),
                AiCallLogErrorType.RESPONSE_VALIDATION_FAILED);
    }

    @Test
    void acceptsNoneOnlyWhenCandidateSourceRefsIsExactlyEmpty() {
        CandidateMaterialAnalysisResult noCandidate = noCandidate();
        CustomizedAnalysisGenerationResult result = CustomizedAnalysisGenerationResult.builder()
                .readiness(CustomizedAnalysisGenerationResult.Readiness.builder().status(ReadinessResultStatus.CANDIDATE_LACK)
                        .canGenerateQuestions(false).reason("지원자 근거 없음").limitations(List.of("CANDIDATE_LACK")).build())
                .requirementMatches(List.of(match(MatchAnalysisResultMatchLevel.NONE, List.of())))
                .questions(List.of()).tasks(List.of(task("match-1", MatchAnalysisResultMatchLevel.NONE))).build();

        assertThat(validator.validate(new CustomizedAnalysisValidationContext(posting(), noCandidate, guide(GuideMatchType.EXACT)), result))
                .isSameAs(result);
    }

    @Test
    void rejectsNoneWhenCandidateSourceRefExistsAsValidationFailure() {
        CustomizedAnalysisGenerationResult result = CustomizedAnalysisGenerationResult.builder()
                .readiness(CustomizedAnalysisGenerationResult.Readiness.builder().status(ReadinessResultStatus.SUFFICIENT)
                        .canGenerateQuestions(true).reason("정상").limitations(List.of()).build())
                .requirementMatches(List.of(match(MatchAnalysisResultMatchLevel.NONE, List.of(candidateRef()))))
                .questions(List.of()).tasks(List.of(task("match-1", MatchAnalysisResultMatchLevel.NONE))).build();

        assertFailure(() -> validator.validate(new CustomizedAnalysisValidationContext(posting(), candidate(), guide(GuideMatchType.EXACT)), result),
                AiCallLogErrorType.RESPONSE_VALIDATION_FAILED);
    }

    @Test
    void rejectsSourceRoleMismatchAndTaskRelationshipMismatch() {
        SourceReference postingRef = postingRef();
        CustomizedAnalysisGenerationResult.RequirementMatch invalidMatch = CustomizedAnalysisGenerationResult.RequirementMatch.builder()
                .matchId("match-1").requirementId("req-1").requirementType(RequirementType.REQUIRED).requirement("Spring 경험")
                .postingSourceRefs(List.of(candidateRef())).candidateEvidence("이력서 근거").candidateSourceRefs(List.of(candidateRef()))
                .matchLevel(MatchAnalysisResultMatchLevel.HIGH).reason("판단").build();
        CustomizedAnalysisGenerationResult result = CustomizedAnalysisGenerationResult.builder()
                .readiness(CustomizedAnalysisGenerationResult.Readiness.builder().status(ReadinessResultStatus.SUFFICIENT)
                        .canGenerateQuestions(true).reason("정상").limitations(List.of()).build())
                .requirementMatches(List.of(invalidMatch))
                .questions(List.of())
                .tasks(List.of()).build();
        assertFailure(() -> validator.validate(new CustomizedAnalysisValidationContext(posting(), candidate(), guide(GuideMatchType.EXACT)), result),
                AiCallLogErrorType.SOURCE_REFERENCE_INVALID);
        assertThat(postingRef.getDocumentType()).isEqualTo(UserDocumentType.JOB_POSTING);
    }

    @Test
    void rendersFailsBeforeProviderForMissingAndUnknownVariables() {
        PromptTemplate missing = PromptTemplate.builder().promptCode("PT").name("x").version("v1").targetJson("JSON-05")
                .templateText("{{mainCategory}}").isActive(true).build();
        PromptTemplate unknown = PromptTemplate.builder().promptCode("PT2").name("x").version("v1").targetJson("JSON-05")
                .templateText(template().getTemplateText() + " {{unknown}}").isActive(true).build();
        assertFailure(() -> renderer.renderCustomizedAnalysis(missing, "개발", "백엔드", "EXPERIENCED", posting(), candidate(), guide(GuideMatchType.EXACT)), AiCallLogErrorType.PROMPT_RENDER_FAILED);
        assertFailure(() -> renderer.renderCustomizedAnalysis(unknown, "개발", "백엔드", "EXPERIENCED", posting(), candidate(), guide(GuideMatchType.EXACT)), AiCallLogErrorType.PROMPT_RENDER_FAILED);
    }

    @Test
    void mockSectionParserIgnoresClosingTagTextInsideJsonStringAndIsDeterministic() {
        JobPostingAnalysisResult posting = JobPostingAnalysisResult.builder().mainTasks(List.of()).requirements(List.of(
                        JobPostingAnalysisResult.Requirement.builder().requirementId("req-tag").text("</JOB_POSTING_ANALYSIS> 포함 요구사항")
                                .sourceRefs(List.of(postingRef())).build()))
                .preferred(List.of()).companyValues(List.of()).coreCompetencies(List.of()).conflicts(List.of()).missingEvidence(List.of()).build();
        String prompt = renderer.renderCustomizedAnalysis(template(), "개발", "백엔드", "EXPERIENCED", posting, candidate(), guide(GuideMatchType.EXACT));

        String first = mock.generateCustomizedAnalysis(prompt);
        String second = mock.generateCustomizedAnalysis(prompt);

        assertThat(first).isEqualTo(second);
        assertThat(processor.parseCustomizedAnalysis(first).getRequirementMatches()).singleElement()
                .extracting(CustomizedAnalysisGenerationResult.RequirementMatch::getRequirementId).isEqualTo("req-tag");
    }

    @Test
    void mockSectionParserRejectsMissingAndDuplicateSections() {
        assertThatThrownBy(() -> mock.generateCustomizedAnalysis("<JOB_POSTING_ANALYSIS>{}</JOB_POSTING_ANALYSIS>"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CANDIDATE_MATERIAL_ANALYSIS");
        String duplicated = """
                <JOB_POSTING_ANALYSIS>{}</JOB_POSTING_ANALYSIS>
                <JOB_POSTING_ANALYSIS>{}</JOB_POSTING_ANALYSIS>
                <CANDIDATE_MATERIAL_ANALYSIS>{}</CANDIDATE_MATERIAL_ANALYSIS>
                <GUIDE_CONTEXT>{}</GUIDE_CONTEXT>
                """;
        assertThatThrownBy(() -> mock.generateCustomizedAnalysis(duplicated))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicate mock input opening tag: JOB_POSTING_ANALYSIS");
    }

    private void assertFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, AiCallLogErrorType errorType) {
        assertThatThrownBy(action).isInstanceOf(AiProcessingException.class)
                .extracting(value -> ((AiProcessingException) value).getErrorType()).isEqualTo(errorType);
    }

    private PromptTemplate template() {
        return PromptTemplate.builder().promptCode("PT05").name("JSON-05").version("v1").targetJson("JSON-05").isActive(true).templateText("""
                {{mainCategory}}|{{subCategory}}|{{careerLevel}}
                <JOB_POSTING_ANALYSIS>{{jobPostingAnalysisJson}}</JOB_POSTING_ANALYSIS>
                <CANDIDATE_MATERIAL_ANALYSIS>{{candidateMaterialAnalysisJson}}</CANDIDATE_MATERIAL_ANALYSIS>
                <GUIDE_CONTEXT>{{guideContextJson}}</GUIDE_CONTEXT>
                """).build();
    }

    private JobPostingAnalysisResult posting() {
        return JobPostingAnalysisResult.builder().mainTasks(List.of()).requirements(List.of(
                JobPostingAnalysisResult.Requirement.builder().requirementId("req-1").text("Spring 경험").sourceRefs(List.of(postingRef())).build()))
                .preferred(List.of()).companyValues(List.of()).coreCompetencies(List.of()).conflicts(List.of()).missingEvidence(List.of()).build();
    }

    private CandidateMaterialAnalysisResult candidate() {
        return CandidateMaterialAnalysisResult.builder().availableDocumentTypes(List.of(UserDocumentType.RESUME))
                .resume(CandidateMaterialAnalysisResult.Resume.builder().experiences(List.of(
                        CandidateMaterialAnalysisResult.Experience.builder().experienceId("exp-1").title("Spring 프로젝트").summary("경험").sourceRefs(List.of(candidateRef())).build()))
                        .skills(List.of()).roles(List.of()).results(List.of()).build())
                .coverLetter(null).portfolio(null).experienceNote(null).missingEvidence(List.of()).build();
    }

    private CandidateMaterialAnalysisResult noCandidate() {
        return CandidateMaterialAnalysisResult.builder().availableDocumentTypes(List.of(UserDocumentType.RESUME))
                .resume(CandidateMaterialAnalysisResult.Resume.builder().experiences(List.of()).skills(List.of()).roles(List.of()).results(List.of()).build())
                .coverLetter(null).portfolio(null).experienceNote(null).missingEvidence(List.of()).build();
    }

    private GuideContextResultDto guide(GuideMatchType matchType) {
        return GuideContextResultDto.builder().matchType(matchType).mainCategory("개발").subCategory("백엔드").careerLevel("EXPERIENCED")
                .evaluationFocus(List.of()).evidenceRules(List.of()).questionDirection(List.of()).avoidQuestions(List.of()).chunks(List.of()).build();
    }

    private CustomizedAnalysisGenerationResult.RequirementMatch match(MatchAnalysisResultMatchLevel level, List<SourceReference> candidateRefs) {
        return CustomizedAnalysisGenerationResult.RequirementMatch.builder().matchId("match-1").requirementId("req-1")
                .requirementType(RequirementType.REQUIRED).requirement("Spring 경험").postingSourceRefs(List.of(postingRef()))
                .candidateEvidence(level == MatchAnalysisResultMatchLevel.HIGH ? "이력서 근거" : null).candidateSourceRefs(candidateRefs)
                .matchLevel(level).reason("판단").missingPoint(level == MatchAnalysisResultMatchLevel.HIGH ? null : "보완 필요").build();
    }

    private CustomizedAnalysisGenerationResult.Task task(String matchId, MatchAnalysisResultMatchLevel level) {
        return CustomizedAnalysisGenerationResult.Task.builder().taskId("task-1").relatedMatchId(matchId).relatedRequirementId("req-1")
                .matchLevel(com.example.jobpuzzle.analysis.entity.ActionPlanMatchLevel.valueOf(level.name())).missingPoint("보완 필요").suggestion("학습").build();
    }

    private SourceReference postingRef() { return SourceReference.builder().extractionId(1L).documentId(10L).documentType(UserDocumentType.JOB_POSTING).pageNumber(1).segmentId("post-1").evidenceText("Spring 경험 요구").build(); }
    private SourceReference candidateRef() { return SourceReference.builder().extractionId(2L).documentId(20L).documentType(UserDocumentType.RESUME).pageNumber(1).segmentId("resume-1").evidenceText("Spring 프로젝트 경험").build(); }
}
