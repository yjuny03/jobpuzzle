package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderResult;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomizedSynthesisResultAssemblerTest {
    private final CustomizedSynthesisResultAssembler assembler = new CustomizedSynthesisResultAssembler();

    @Test
    void restoresServerOwnedIdentityAndIds() {
        var result = assembler.assemble(provider("candidate-1"), authority(), GuideMatchType.EXACT);

        assertThat(result.getRequirementMatches()).singleElement().satisfies(match -> {
            assertThat(match.getMatchId()).isEqualTo("match-1");
            assertThat(match.getRequirement()).isEqualTo("Java 경험");
            assertThat(match.getRequirementType()).isEqualTo(RequirementType.REQUIRED);
            assertThat(match.getCandidateEvidenceIds()).isNull();
            assertThat(match.getCandidateSourceRefs()).singleElement().satisfies(source -> {
                assertThat(source.getDocumentId()).isEqualTo(20L);
                assertThat(source.getEvidenceText()).isEqualTo("후보 근거");
            });
        });
        assertThat(result.getQuestions()).singleElement().satisfies(question -> {
            assertThat(question.getQuestionId()).isEqualTo("question-1");
            assertThat(question.getRelatedMatchId()).isEqualTo("match-1");
            assertThat(question.getSourceRefs()).hasSize(2);
        });
    }

    @Test
    void rejectsCandidateEvidenceFromAnotherRequirement() {
        assertThatThrownBy(() -> assembler.assemble(provider("candidate-other"), authority(), GuideMatchType.EXACT))
                .isInstanceOf(AiProcessingException.class)
                .satisfies(error -> assertThat(((AiProcessingException) error).getErrorType())
                        .isEqualTo(AiCallLogErrorType.SOURCE_REFERENCE_INVALID));
    }

    @Test
    void restoresMissingCandidateReferenceForRelatedQuestion() {
        CustomizedSynthesisProviderResult provider = provider("candidate-1");
        provider.getQuestions().get(0).setEvidenceIds(List.of("posting-1"));

        var result = assembler.assemble(provider, authority(), GuideMatchType.EXACT);

        assertThat(result.getQuestions()).singleElement()
                .satisfies(question -> assertThat(question.getSourceRefs()).hasSize(2));
    }

    @Test
    void serverAddsStructuralLimitationsForUnavailableInputs() {
        CustomizedSynthesisProviderResult provider = CustomizedSynthesisProviderResult.builder()
                .readiness(CustomizedSynthesisProviderResult.Readiness.builder()
                        .reason("입력 부족").limitations(List.of()).build())
                .requirementMatches(List.of()).questions(List.of()).tasks(List.of()).build();

        var result = assembler.assemble(provider,
                new CustomizedSynthesisEvidenceCatalog(List.of(), List.of()), GuideMatchType.NONE);

        assertThat(result.getReadiness().getLimitations()).hasSize(3);
        assertThat(result.getReadiness().isCanGenerateQuestions()).isFalse();
    }

    @Test
    void preservesMatchesAndDisablesQuestionsWhenProviderReturnsNoUsableQuestion() {
        CustomizedSynthesisProviderResult provider = provider("candidate-1");
        provider.setQuestions(List.of(CustomizedSynthesisProviderResult.Question.builder()
                .relatedRequirementId("req-1").questionType(InterviewQuestionType.COMPANY_FIT)
                .question("placeholder?").intent("임시 문구")
                .evaluationFocus(List.of(InterviewQuestionEvaluationFocus.requirementConnection))
                .evidenceIds(List.of("candidate-1")).build()));

        var result = assembler.assemble(provider, authority(), GuideMatchType.EXACT);

        assertThat(result.getRequirementMatches()).hasSize(1);
        assertThat(result.getQuestions()).isEmpty();
        assertThat(result.getReadiness().isCanGenerateQuestions()).isFalse();
        assertThat(result.getReadiness().getLimitations())
                .contains("질문 생성 결과에서 사용할 수 있는 근거 기반 질문을 확인하지 못했습니다.");
    }

    @Test
    void rejectsDuplicateTasksForSameNonHighRequirement() {
        CustomizedSynthesisProviderResult provider = provider("candidate-1");
        var match = provider.getRequirementMatches().get(0);
        match.setMatchLevel(MatchAnalysisResultMatchLevel.NONE);
        match.setCandidateEvidence(null);
        match.setCandidateEvidenceIds(List.of());
        match.setMissingPoint("근거 부족");
        var task = CustomizedSynthesisProviderResult.Task.builder()
                .relatedRequirementId("req-1").missingPoint("근거 부족").suggestion("보완").build();
        provider.setTasks(List.of(task, task));

        assertThatThrownBy(() -> assembler.assemble(provider, authority(), GuideMatchType.EXACT))
                .isInstanceOf(AiProcessingException.class)
                .hasMessage("exactly one task is required for non-HIGH match");
    }

    private CustomizedSynthesisProviderResult provider(String selectedCandidateId) {
        return CustomizedSynthesisProviderResult.builder()
                .readiness(CustomizedSynthesisProviderResult.Readiness.builder()
                        .reason("준비됨").limitations(List.of()).build())
                .requirementMatches(List.of(CustomizedSynthesisProviderResult.RequirementMatch.builder()
                        .requirementId("req-1").matchLevel(MatchAnalysisResultMatchLevel.HIGH)
                        .reason("근거 있음").candidateEvidence("후보 근거")
                        .candidateEvidenceIds(List.of(selectedCandidateId)).build()))
                .questions(List.of(CustomizedSynthesisProviderResult.Question.builder()
                        .relatedRequirementId("req-1").questionType(InterviewQuestionType.COMPANY_FIT)
                        .question("설명해주세요").intent("검증")
                        .evaluationFocus(List.of(InterviewQuestionEvaluationFocus.requirementConnection))
                        .evidenceIds(List.of("posting-1", "candidate-1")).build()))
                .tasks(List.of()).build();
    }

    private CustomizedSynthesisEvidenceCatalog authority() {
        return new CustomizedSynthesisEvidenceCatalog(
                List.of(new CustomizedSynthesisEvidenceCatalog.RequirementItem(
                        "req-1", RequirementType.REQUIRED, "Java 경험",
                        List.of("posting-1"), List.of("candidate-1"))),
                List.of(
                        new CustomizedSynthesisEvidenceCatalog.EvidenceItem("posting-1",
                                CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING,
                                source(10L, UserDocumentType.JOB_POSTING, "공고 근거")),
                        new CustomizedSynthesisEvidenceCatalog.EvidenceItem("candidate-1",
                                CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE,
                                source(20L, UserDocumentType.RESUME, "후보 근거")),
                        new CustomizedSynthesisEvidenceCatalog.EvidenceItem("candidate-other",
                                CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE,
                                source(21L, UserDocumentType.PORTFOLIO, "다른 근거"))));
    }

    private SourceReference source(Long documentId, UserDocumentType type, String text) {
        return SourceReference.builder().extractionId(documentId).documentId(documentId)
                .documentType(type).pageNumber(1).segmentId("segment-" + documentId)
                .evidenceText(text).build();
    }
}
