package com.example.jobpuzzle.ai.validation;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.analysis.entity.ReadinessResultStatus;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.rag.dto.RetrievedEvidenceContextDto;
import com.example.jobpuzzle.guide.entity.GuideMatchType;
import com.example.jobpuzzle.interview.entity.InterviewQuestionReviewStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

// JSON-05 응답의 요구사항·준비도·질문·과제·근거 관계를 저장 전에 검증한다.
@Component
public class CustomizedAnalysisResponseValidator {

    public CustomizedAnalysisGenerationResult validate(CustomizedAnalysisValidationContext context,
                                                        CustomizedAnalysisGenerationResult result) {
        if (result == null || result.getReadiness() == null) fail("readiness is required");
        requireList("requirementMatches", result.getRequirementMatches());
        requireList("questions", result.getQuestions());
        requireList("tasks", result.getTasks());
        Map<String, RequirementInput> inputs = requirements(context.jobPostingAnalysis());
        Map<String, List<SourceReference>> candidateRefs = candidateRefs(context.retrievedEvidence());
        validateReadiness(context.guideContext(), inputs, allCandidateRefs(candidateRefs), result.getReadiness());
        Map<String, CustomizedAnalysisGenerationResult.RequirementMatch> matches = validateMatches(inputs, candidateRefs, result.getRequirementMatches());
        validateQuestions(result.getReadiness().isCanGenerateQuestions(), matches, inputs.keySet(), inputs, allCandidateRefs(candidateRefs), result.getQuestions());
        validateTasks(matches, result.getTasks());
        return result;
    }

    // JSON-01의 REQUIRED/PREFERRED 요구사항이 JSON-05에 정확히 한 번씩 대응되는지 확인한다.
    private Map<String, CustomizedAnalysisGenerationResult.RequirementMatch> validateMatches(
            Map<String, RequirementInput> inputs, Map<String, List<SourceReference>> candidateRefs,
            List<CustomizedAnalysisGenerationResult.RequirementMatch> matches) {
        Map<String, CustomizedAnalysisGenerationResult.RequirementMatch> byRequirement = new LinkedHashMap<>();
        Set<String> matchIds = new HashSet<>();
        for (CustomizedAnalysisGenerationResult.RequirementMatch match : matches) {
            requireText("matchId", match.getMatchId());
            if (!matchIds.add(match.getMatchId())) fail("duplicate matchId");
            requireText("requirementId", match.getRequirementId());
            if (byRequirement.put(match.getRequirementId(), match) != null) fail("duplicate requirementId");
            RequirementInput input = inputs.get(match.getRequirementId());
            if (input == null) fail("unknown requirementId");
            if (match.getRequirementType() != input.type()) fail("requirementType mismatch");
            if (!normalize(match.getRequirement()).equals(normalize(input.requirement()))) fail("requirement text mismatch");
            requireText("reason", match.getReason());
            if (match.getMatchLevel() == null) fail("matchLevel is required");
            validateReferences(match.getPostingSourceRefs(), input.sourceRefs(), "postingSourceRefs", true);
            validateCandidateEvidence(match, candidateRefs.getOrDefault(match.getRequirementId(), List.of()));
            boolean high = match.getMatchLevel() == MatchAnalysisResultMatchLevel.HIGH;
            if (high && !blank(match.getMissingPoint())) fail("HIGH missingPoint must be blank");
            if (!high) requireText("missingPoint", match.getMissingPoint());
        }
        if (!byRequirement.keySet().equals(inputs.keySet())) fail("JSON-01 requirements must be included exactly once");
        return matches.stream().collect(java.util.stream.Collectors.toMap(
                CustomizedAnalysisGenerationResult.RequirementMatch::getMatchId, Function.identity()));
    }

    private void validateCandidateEvidence(CustomizedAnalysisGenerationResult.RequirementMatch match, List<SourceReference> candidateRefs) {
        boolean evidenceRequired = Set.of(MatchAnalysisResultMatchLevel.HIGH, MatchAnalysisResultMatchLevel.MEDIUM, MatchAnalysisResultMatchLevel.LOW)
                .contains(match.getMatchLevel());
        if (evidenceRequired) {
            requireText("candidateEvidence", match.getCandidateEvidence());
            validateReferences(match.getCandidateSourceRefs(), candidateRefs, "candidateSourceRefs", true);
        } else if (match.getMatchLevel() == MatchAnalysisResultMatchLevel.NONE) {
            if (!blank(match.getCandidateEvidence())) fail("candidateEvidence must be blank for NONE");
            requireList("candidateSourceRefs", match.getCandidateSourceRefs());
            if (!match.getCandidateSourceRefs().isEmpty()) fail("candidateSourceRefs must be empty for NONE");
        } else {
            if (!blank(match.getCandidateEvidence())) fail("candidateEvidence must be blank for " + match.getMatchLevel());
            validateReferences(match.getCandidateSourceRefs(), candidateRefs, "candidateSourceRefs", false);
        }
    }

    // readiness 상태와 질문 생성 가능 여부는 입력 부족 사유의 우선순위와 함께 검증한다.
    private void validateReadiness(com.example.jobpuzzle.guide.dto.GuideContextResultDto guide,
                                   Map<String, RequirementInput> inputs, List<SourceReference> candidateRefs,
                                   CustomizedAnalysisGenerationResult.Readiness readiness) {
        if (readiness.getStatus() == null) fail("readiness.status is required");
        requireText("readiness.reason", readiness.getReason());
        requireList("readiness.limitations", readiness.getLimitations());
        readiness.getLimitations().forEach(value -> requireText("readiness.limitations", value));
        List<ReadinessResultStatus> lacks = new ArrayList<>();
        if (inputs.isEmpty()) lacks.add(ReadinessResultStatus.POSTING_LACK);
        if (candidateRefs.isEmpty()) lacks.add(ReadinessResultStatus.CANDIDATE_LACK);
        if (guide == null || guide.getMatchType() == GuideMatchType.NONE) lacks.add(ReadinessResultStatus.GUIDE_LACK);
        ReadinessResultStatus expected = lacks.isEmpty() ? null : lacks.get(0);
        if (expected != null && readiness.getStatus() != expected) fail("readiness status priority mismatch");
        if (expected == null && guide != null && Set.of(GuideMatchType.FALLBACK_PARENT_CATEGORY, GuideMatchType.FALLBACK_COMMON).contains(guide.getMatchType())
                && readiness.getStatus() != ReadinessResultStatus.PARTIAL) fail("limited guide must be PARTIAL");
        if (expected == null && guide != null && Set.of(GuideMatchType.EXACT, GuideMatchType.FALLBACK_SAME_SUBCATEGORY).contains(guide.getMatchType())
                && !Set.of(ReadinessResultStatus.SUFFICIENT, ReadinessResultStatus.PARTIAL).contains(readiness.getStatus())) fail("exact guide requires SUFFICIENT or PARTIAL");
        boolean expectedQuestions = readiness.getStatus() == ReadinessResultStatus.SUFFICIENT || readiness.getStatus() == ReadinessResultStatus.PARTIAL;
        if (readiness.isCanGenerateQuestions() != expectedQuestions) fail("canGenerateQuestions mismatch");
        if (lacks.size() > 1 && readiness.getLimitations().isEmpty()) fail("secondary readiness limitations are required");
    }

    // 질문은 실제 requirementMatch와 JSON-01·02 근거를 연결한 PASS 결과만 허용한다.
    private void validateQuestions(boolean canGenerateQuestions,
                                   Map<String, CustomizedAnalysisGenerationResult.RequirementMatch> matches,
                                   Set<String> requirementIds, Map<String, RequirementInput> inputs,
                                   List<SourceReference> candidateRefs,
                                   List<CustomizedAnalysisGenerationResult.Question> questions) {
        if (!canGenerateQuestions) {
            if (!questions.isEmpty()) fail("questions must be empty when generation is disabled");
            return;
        }
        if (questions.isEmpty() || questions.size() > 10) fail("questions must contain 1 to 10 PASS items");
        Set<String> ids = new HashSet<>();
        List<SourceReference> allRefs = new ArrayList<>(); inputs.values().forEach(value -> allRefs.addAll(value.sourceRefs())); allRefs.addAll(candidateRefs);
        for (CustomizedAnalysisGenerationResult.Question question : questions) {
            requireText("questionId", question.getQuestionId()); if (!ids.add(question.getQuestionId())) fail("duplicate questionId");
            requireText("question", question.getQuestion()); requireText("intent", question.getIntent());
            if (question.getQuestionType() == null) fail("questionType is required");
            requireList("evaluationFocus", question.getEvaluationFocus()); if (question.getEvaluationFocus().isEmpty()) fail("evaluationFocus must not be empty");
            if (new HashSet<>(question.getEvaluationFocus()).size() != question.getEvaluationFocus().size()) fail("duplicate evaluationFocus");
            if (question.getReviewStatus() != InterviewQuestionReviewStatus.PASS) fail("only PASS questions are allowed");
            CustomizedAnalysisGenerationResult.RequirementMatch match = null;
            if (question.getRelatedMatchId() != null) {
                match = matches.get(question.getRelatedMatchId()); if (match == null) fail("unknown relatedMatchId");
            }
            if (question.getRelatedRequirementId() != null && !requirementIds.contains(question.getRelatedRequirementId())) fail("unknown relatedRequirementId");
            if (match != null && question.getRelatedRequirementId() != null && !match.getRequirementId().equals(question.getRelatedRequirementId())) fail("question relation mismatch");
            validateReferences(question.getSourceRefs(), allRefs, "question.sourceRefs", true);
        }
    }

    // 과제는 실제 연결 결과와 같은 등급·요구사항을 참조하며 HIGH에는 존재할 수 없다.
    private void validateTasks(Map<String, CustomizedAnalysisGenerationResult.RequirementMatch> matches,
                               List<CustomizedAnalysisGenerationResult.Task> tasks) {
        Set<String> ids = new HashSet<>();
        Map<String, Integer> counts = new HashMap<>();
        for (CustomizedAnalysisGenerationResult.Task task : tasks) {
            requireText("taskId", task.getTaskId()); if (!ids.add(task.getTaskId())) fail("duplicate taskId");
            requireText("relatedMatchId", task.getRelatedMatchId()); requireText("relatedRequirementId", task.getRelatedRequirementId());
            CustomizedAnalysisGenerationResult.RequirementMatch match = matches.get(task.getRelatedMatchId());
            if (match == null) fail("unknown task relatedMatchId");
            if (!match.getRequirementId().equals(task.getRelatedRequirementId())) fail("task requirement mismatch");
            if (task.getMatchLevel() == null || !task.getMatchLevel().name().equals(match.getMatchLevel().name())) fail("task matchLevel mismatch");
            if (match.getMatchLevel() == MatchAnalysisResultMatchLevel.HIGH) fail("HIGH task is forbidden");
            requireText("task.missingPoint", task.getMissingPoint()); requireText("task.suggestion", task.getSuggestion());
            counts.merge(match.getMatchId(), 1, Integer::sum);
        }
        for (CustomizedAnalysisGenerationResult.RequirementMatch match : matches.values()) {
            if (match.getMatchLevel() != MatchAnalysisResultMatchLevel.HIGH && counts.getOrDefault(match.getMatchId(), 0) == 0) fail("task is required for " + match.getMatchLevel());
        }
    }

    // sourceRefs는 JSON-01·02에 이미 저장된 동일 식별 조합과 발췌문만 echo할 수 있다.
    private void validateReferences(List<SourceReference> values, List<SourceReference> allowed, String field, boolean required) {
        if (values == null) fail(field + " must not be null");
        if (required && values.isEmpty()) source(field + " must not be empty");
        for (SourceReference value : values) {
            if (value == null || value.getExtractionId() == null || value.getDocumentId() == null || value.getDocumentType() == null
                    || blank(value.getEvidenceText())) source(field + " missing required source field");
            SourceReference original = allowed.stream().filter(candidate -> sameIdentity(candidate, value)).findFirst().orElse(null);
            if (original == null) source(field + " unknown source identity");
            if (!normalize(original.getEvidenceText()).contains(normalize(value.getEvidenceText()))) source(field + " evidenceText mismatch");
        }
    }

    private Map<String, RequirementInput> requirements(JobPostingAnalysisResult result) {
        if (result == null || result.getRequirements() == null || result.getPreferred() == null) fail("JSON-01 requirements are required");
        Map<String, RequirementInput> values = new LinkedHashMap<>();
        result.getRequirements().forEach(value -> putRequirement(values, value, RequirementType.REQUIRED));
        result.getPreferred().forEach(value -> putRequirement(values, value, RequirementType.PREFERRED));
        return values;
    }

    private void putRequirement(Map<String, RequirementInput> values, JobPostingAnalysisResult.Requirement value, RequirementType type) {
        if (value == null || blank(value.getRequirementId()) || blank(value.getText()) || value.getSourceRefs() == null) fail("invalid JSON-01 requirement");
        if (values.put(value.getRequirementId(), new RequirementInput(type, value.getText(), value.getSourceRefs())) != null) fail("duplicate JSON-01 requirementId");
    }

    // retrieval-selected chunk만 requirement별 candidateSourceRefs의 허용 근거로 변환한다.
    private Map<String, List<SourceReference>> candidateRefs(RetrievedEvidenceContextDto evidence) {
        if (evidence == null || evidence.requirements() == null) fail("retrieved evidence is required");
        Map<String, List<SourceReference>> refs = new HashMap<>();
        for (RetrievedEvidenceContextDto.RequirementEvidence requirement : evidence.requirements()) {
            if (requirement == null || blank(requirement.requirementId()) || requirement.chunks() == null
                    || refs.containsKey(requirement.requirementId())) fail("invalid retrieved evidence");
            List<SourceReference> values = requirement.chunks().stream().map(chunk -> SourceReference.builder()
                    .extractionId(chunk.extractionId()).documentId(chunk.documentId()).documentType(chunk.documentType())
                    .pageNumber(chunk.pageStart()).segmentId(null).evidenceText(chunk.content()).build()).toList();
            refs.put(requirement.requirementId(), values);
        }
        return refs;
    }

    // readiness와 질문은 전체 retrieval 근거를 보되 requirement match는 개별 범위를 유지한다.
    private List<SourceReference> allCandidateRefs(Map<String, List<SourceReference>> values) {
        return values.values().stream().flatMap(Collection::stream).toList();
    }
    private boolean sameIdentity(SourceReference left, SourceReference right) { return Objects.equals(left.getExtractionId(), right.getExtractionId()) && Objects.equals(left.getDocumentId(), right.getDocumentId()) && left.getDocumentType() == right.getDocumentType() && Objects.equals(left.getPageNumber(), right.getPageNumber()) && Objects.equals(left.getSegmentId(), right.getSegmentId()); }
    private String normalize(String value) { return value == null ? "" : value.replaceAll("\\s+", " ").trim(); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private void requireText(String field, String value) { if (blank(value)) fail(field + " must not be blank"); }
    private void requireList(String field, List<?> value) { if (value == null) fail(field + " must not be null"); }
    private void fail(String message) { throw new AiProcessingException(AiCallLogErrorType.RESPONSE_VALIDATION_FAILED, message); }
    private void source(String message) { throw new AiProcessingException(AiCallLogErrorType.SOURCE_REFERENCE_INVALID, message); }
    private record RequirementInput(RequirementType type, String requirement, List<SourceReference> sourceRefs) { }
}
