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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomizedSynthesisV15ResultAssemblerTest {
    private final CustomizedSynthesisV15ResultAssembler assembler =
            new CustomizedSynthesisV15ResultAssembler(new CustomizedSynthesisResultAssembler());

    @Test
    void convertsFixedSlotsWithoutChangingDownstreamContract() {
        var result = assembler.assemble(provider(), input(true), authority(), GuideMatchType.EXACT);

        assertThat(result.getRequirementMatches()).singleElement()
                .satisfies(value -> assertThat(value.getRequirement()).isEqualTo("Java 경험"));
        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getTasks()).isEmpty();
    }

    @Test
    void rejectsMissingRequirementKey() {
        var provider = provider();
        provider.setRequirementMatchesById(Map.of());

        assertThatThrownBy(() -> assembler.assemble(provider, input(true), authority(), GuideMatchType.EXACT))
                .hasMessageContaining("every requirement exactly once");
    }

    @Test
    void rejectsTaskThatContradictsHighMatch() {
        var provider = provider();
        provider.setTasksByRequirementId(Map.of("req-1",
                new CustomizedSynthesisV15ProviderResult.TaskSlot(true, "부족", "보완")));

        assertThatThrownBy(() -> assembler.assemble(provider, input(true), authority(), GuideMatchType.EXACT))
                .hasMessageContaining("HIGH task slot");
    }

    @Test
    void rejectsMissingPolicyRequiredQuestion() {
        var provider = provider();
        provider.setPrimaryQuestion(null);

        assertThatThrownBy(() -> assembler.assemble(provider, input(true), authority(), GuideMatchType.EXACT))
                .hasMessageContaining("primaryQuestion is required");
    }

    private CustomizedSynthesisV15ProviderResult provider() {
        Map<String, CustomizedSynthesisV15ProviderResult.MatchSlot> matches = new LinkedHashMap<>();
        matches.put("req-1", new CustomizedSynthesisV15ProviderResult.MatchSlot(
                MatchAnalysisResultMatchLevel.HIGH, "근거 있음", "", "후보 근거", List.of("cand-1")));
        return new CustomizedSynthesisV15ProviderResult(
                new CustomizedSynthesisV15ProviderResult.Readiness("준비됨", List.of()),
                matches,
                new CustomizedSynthesisV15ProviderResult.QuestionSlot(
                        "req-1", InterviewQuestionType.COMPANY_FIT, "설명해주세요", "검증",
                        List.of(InterviewQuestionEvaluationFocus.requirementConnection),
                        List.of("post-1", "cand-1")),
                Map.of("req-1", new CustomizedSynthesisV15ProviderResult.TaskSlot(false, "", "")));
    }

    private CustomizedSynthesisProviderInput input(boolean questions) {
        return new CustomizedSynthesisProviderInput(
                null,
                List.of(new CustomizedSynthesisProviderInput.RequirementItem(
                        "req-1", RequirementType.REQUIRED, "Java 경험", List.of("post-1"), List.of("cand-1"))),
                List.of(), null,
                new CustomizedSynthesisProviderEvidenceCatalog(List.of(
                        new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                                "post-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING, "공고 근거"),
                        new CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem(
                                "cand-1", CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE, "후보 근거"))),
                new CustomizedSynthesisProviderInput.GenerationPolicy(questions, questions ? 1 : 0));
    }

    private CustomizedSynthesisEvidenceCatalog authority() {
        return new CustomizedSynthesisEvidenceCatalog(
                List.of(new CustomizedSynthesisEvidenceCatalog.RequirementItem(
                        "req-1", RequirementType.REQUIRED, "Java 경험", List.of("post-1"), List.of("cand-1"))),
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
