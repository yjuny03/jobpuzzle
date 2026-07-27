package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.synthesis.dto.*;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomizedSynthesisV16ResultAssemblerTest {
    private final CustomizedSynthesisV16ResultAssembler assembler =
            new CustomizedSynthesisV16ResultAssembler(new CustomizedSynthesisV15ResultAssembler(
                    new CustomizedSynthesisResultAssembler()));

    @Test
    void restoresFlatMapsIntoExistingDownstreamResult() {
        var result = assembler.assemble(provider(), input(), authority(), GuideMatchType.EXACT);

        assertThat(result.getRequirementMatches()).singleElement().satisfies(match -> {
            assertThat(match.getRequirementId()).isEqualTo("req-1");
            assertThat(match.getCandidateSourceRefs()).hasSize(1);
        });
        assertThat(result.getQuestions()).singleElement()
                .satisfies(question -> assertThat(question.getRelatedRequirementId()).isNull());
        assertThat(result.getTasks()).isEmpty();
    }

    @Test
    void rejectsMissingKeyInAnyParallelMap() {
        var provider = provider();
        provider.setMatchReasonsById(Map.of());

        assertThatThrownBy(() -> assembler.assemble(provider, input(), authority(), GuideMatchType.EXACT))
                .hasMessageContaining("every requirement exactly once");
    }

    private CustomizedSynthesisV16ProviderResult provider() {
        return new CustomizedSynthesisV16ProviderResult(
                new CustomizedSynthesisV16ProviderResult.Readiness("준비됨", List.of()),
                Map.of("req-1", MatchAnalysisResultMatchLevel.HIGH),
                Map.of("req-1", "근거 있음"), Map.of("req-1", ""),
                Map.of("req-1", "후보 근거"), Map.of("req-1", "cand-1"),
                new CustomizedSynthesisV16ProviderResult.QuestionSlot(
                        "", InterviewQuestionType.COMPANY_FIT, "설명해주세요", "검증",
                        InterviewQuestionEvaluationFocus.requirementConnection, "cand-1"),
                Map.of("req-1", false), Map.of("req-1", ""), Map.of("req-1", ""));
    }

    private CustomizedSynthesisProviderInput input() {
        return new CustomizedSynthesisProviderInput(null,
                List.of(new CustomizedSynthesisProviderInput.RequirementItem(
                        "req-1", RequirementType.REQUIRED, "Java", List.of("post-1"), List.of("cand-1"))),
                List.of(), null, new CustomizedSynthesisProviderEvidenceCatalog(List.of(
                new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        "post-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING, "공고 근거"),
                new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                        "cand-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE, "후보 근거"))),
                new CustomizedSynthesisProviderInput.GenerationPolicy(true, 1));
    }

    private CustomizedSynthesisEvidenceCatalog authority() {
        return new CustomizedSynthesisEvidenceCatalog(
                List.of(new CustomizedSynthesisEvidenceCatalog.RequirementItem(
                        "req-1", RequirementType.REQUIRED, "Java", List.of("post-1"), List.of("cand-1"))),
                List.of(
                        new CustomizedSynthesisEvidenceCatalog.EvidenceItem("post-1",
                                CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING,
                                source(1L, UserDocumentType.JOB_POSTING, "공고 근거")),
                        new CustomizedSynthesisEvidenceCatalog.EvidenceItem("cand-1",
                                CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE,
                                source(2L, UserDocumentType.RESUME, "후보 근거"))));
    }

    private SourceReference source(Long id, UserDocumentType type, String text) {
        return SourceReference.builder().extractionId(id).documentId(id).documentType(type)
                .pageNumber(1).segmentId("segment-" + id).evidenceText(text).build();
    }
}
