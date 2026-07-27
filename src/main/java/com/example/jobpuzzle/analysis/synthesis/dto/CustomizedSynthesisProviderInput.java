package com.example.jobpuzzle.analysis.synthesis.dto;

import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.guide.entity.GuideMatchType;

import java.util.List;

/** JSON-05 v1.3 provider에 직렬화하는 유일한 입력 DTO다. */
public record CustomizedSynthesisProviderInput(
        JobContext jobContext,
        List<RequirementItem> requirementCatalog,
        List<CandidateContextItem> candidateProjection,
        GuideProjection guideProjection,
        CustomizedSynthesisProviderEvidenceCatalog evidenceCatalog,
        GenerationPolicy generationPolicy
) {
    public CustomizedSynthesisProviderInput {
        requirementCatalog = copy(requirementCatalog);
        candidateProjection = copy(candidateProjection);
    }

    public record JobContext(String mainCategory, String subCategory, String careerLevel) {
    }

    public record RequirementItem(
            String requirementId,
            RequirementType requirementType,
            String requirementText,
            List<String> postingEvidenceIds,
            List<String> allowedCandidateEvidenceIds
    ) {
        public RequirementItem {
            postingEvidenceIds = copy(postingEvidenceIds);
            allowedCandidateEvidenceIds = copy(allowedCandidateEvidenceIds);
        }
    }

    public record CandidateContextItem(
            String candidateItemId,
            CandidateContextType candidateContextType,
            String summary,
            List<String> evidenceIds
    ) {
        public CandidateContextItem {
            evidenceIds = copy(evidenceIds);
        }
    }

    public enum CandidateContextType {
        EXPERIENCE,
        SKILL,
        ROLE,
        RESULT,
        COVER_LETTER,
        PROJECT,
        STAR
    }

    public record GuideProjection(
            String guideCode,
            String version,
            GuideMatchType matchType,
            String applicableScope,
            List<String> evaluationFocus,
            List<String> evidenceRules,
            List<String> questionDirection,
            List<String> avoidQuestions,
            List<GuideChunk> chunks
    ) {
        public GuideProjection {
            evaluationFocus = copy(evaluationFocus);
            evidenceRules = copy(evidenceRules);
            questionDirection = copy(questionDirection);
            avoidQuestions = copy(avoidQuestions);
            chunks = copy(chunks);
        }
    }

    public record GuideChunk(String chunkId, String title, String content) {
    }

    public record GenerationPolicy(
            boolean questionGenerationEnabled,
            int requiredQuestionCount
    ) {
        public GenerationPolicy {
            if (requiredQuestionCount < 0 || requiredQuestionCount > 1
                    || questionGenerationEnabled != (requiredQuestionCount == 1)) {
                throw new IllegalArgumentException("invalid JSON-05 generation policy");
            }
        }
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
