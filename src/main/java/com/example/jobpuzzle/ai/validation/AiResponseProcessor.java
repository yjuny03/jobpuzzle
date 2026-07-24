package com.example.jobpuzzle.ai.validation;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.dto.InterviewQuestionGenerationResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Provider 원시 JSON을 엄격 파싱하고 v13 구조·근거 계약을 검증한다.
@Component
public class AiResponseProcessor {

    private static final Pattern JSON_CODE_BLOCK = Pattern.compile("^```json\\s*\\n([\\s\\S]*)\\n?```$");
    private static final Set<UserDocumentType> JOB_POSTING_TYPES = Set.of(
            UserDocumentType.JOB_POSTING, UserDocumentType.COMPANY_INFO
    );
    private static final Set<UserDocumentType> CANDIDATE_TYPES = Set.of(
            UserDocumentType.RESUME, UserDocumentType.COVER_LETTER,
            UserDocumentType.PORTFOLIO, UserDocumentType.EXPERIENCE_NOTE
    );

    private final ObjectMapper strictObjectMapper;
    private final AnalysisSourceMarkerParser markerParser;

    public AiResponseProcessor(ObjectMapper objectMapper, AnalysisSourceMarkerParser markerParser) {
        this.strictObjectMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        this.markerParser = markerParser;
    }

    // JSON-01 응답은 공고·회사정보 marker에 대해서만 구조와 근거를 검증한다.
    public JobPostingAnalysisResult parseJobPosting(
            String rawJson,
            List<AnalysisInputSnapshotContextSource> sources
    ) {
        JobPostingAnalysisResult result = parse(rawJson, JobPostingAnalysisResult.class, "JSON-01");
        List<AnalysisSourceMarkerParser.SourceMarker> markers = markerParser.parseSources(sources);
        requireList("mainTasks", result.getMainTasks());
        requireList("requirements", result.getRequirements());
        requireList("preferred", result.getPreferred());
        requireList("companyValues", result.getCompanyValues());
        requireList("coreCompetencies", result.getCoreCompetencies());
        requireList("conflicts", result.getConflicts());
        requireList("missingEvidence", result.getMissingEvidence());
        unique("itemId", concat(result.getMainTasks(), result.getCompanyValues(), result.getCoreCompetencies()), JobPostingAnalysisResult.Item::getItemId);
        unique("requirementId", concat(result.getRequirements(), result.getPreferred()), JobPostingAnalysisResult.Requirement::getRequirementId);
        validateItems(result.getMainTasks(), markers, JOB_POSTING_TYPES, JobPostingAnalysisResult.Item::getItemId, JobPostingAnalysisResult.Item::getText, JobPostingAnalysisResult.Item::getSourceRefs);
        validateItems(result.getRequirements(), markers, JOB_POSTING_TYPES, JobPostingAnalysisResult.Requirement::getRequirementId, JobPostingAnalysisResult.Requirement::getText, JobPostingAnalysisResult.Requirement::getSourceRefs);
        validateItems(result.getPreferred(), markers, JOB_POSTING_TYPES, JobPostingAnalysisResult.Requirement::getRequirementId, JobPostingAnalysisResult.Requirement::getText, JobPostingAnalysisResult.Requirement::getSourceRefs);
        validateItems(result.getCompanyValues(), markers, JOB_POSTING_TYPES, JobPostingAnalysisResult.Item::getItemId, JobPostingAnalysisResult.Item::getText, JobPostingAnalysisResult.Item::getSourceRefs);
        validateItems(result.getCoreCompetencies(), markers, JOB_POSTING_TYPES, JobPostingAnalysisResult.Item::getItemId, JobPostingAnalysisResult.Item::getText, JobPostingAnalysisResult.Item::getSourceRefs);
        for (JobPostingAnalysisResult.Conflict conflict : result.getConflicts()) {
            requireText("conflicts.field", conflict.getField());
            requireText("conflicts.postingValue", conflict.getPostingValue());
            requireText("conflicts.companyInfoValue", conflict.getCompanyInfoValue());
            requireText("conflicts.appliedValue", conflict.getAppliedValue());
            validateRefs(conflict.getSourceRefs(), markers, JOB_POSTING_TYPES, "conflicts.sourceRefs");
            Set<UserDocumentType> types = conflict.getSourceRefs().stream().map(SourceReference::getDocumentType).collect(java.util.stream.Collectors.toSet());
            if (!types.contains(UserDocumentType.JOB_POSTING) || !types.contains(UserDocumentType.COMPANY_INFO)) {
                invalidSource("conflicts.sourceRefs", "requires JOB_POSTING and COMPANY_INFO");
            }
        }
        validateMissingEvidence(result.getMissingEvidence(), "missingEvidence");
        return result;
    }

    // JSON-02 응답은 실제 선택된 지원자 자료 유형과 marker를 모두 echo-only로 검증한다.
    public CandidateMaterialAnalysisResult parseCandidateMaterial(
            String rawJson,
            List<AnalysisInputSnapshotContextSource> sources
    ) {
        CandidateMaterialAnalysisResult result = parse(rawJson, CandidateMaterialAnalysisResult.class, "JSON-02");
        List<AnalysisSourceMarkerParser.SourceMarker> markers = markerParser.parseSources(sources);
        Set<UserDocumentType> actualTypes = sources.stream().map(AnalysisInputSnapshotContextSource::getDocumentType)
                .collect(java.util.stream.Collectors.toSet());
        if (!CANDIDATE_TYPES.containsAll(actualTypes)) {
            throw validation("invalid candidate document type");
        }
        requireList("availableDocumentTypes", result.getAvailableDocumentTypes());
        Set<UserDocumentType> returnedTypes = new HashSet<>(result.getAvailableDocumentTypes());
        if (returnedTypes.size() != result.getAvailableDocumentTypes().size() || !returnedTypes.equals(actualTypes)) {
            throw validation("availableDocumentTypes mismatch");
        }
        requireList("missingEvidence", result.getMissingEvidence());
        validateResume(result.getResume(), actualTypes.contains(UserDocumentType.RESUME), markers);
        validateCoverLetter(result.getCoverLetter(), actualTypes.contains(UserDocumentType.COVER_LETTER), markers);
        validatePortfolio(result.getPortfolio(), actualTypes.contains(UserDocumentType.PORTFOLIO), markers);
        validateExperienceNote(result.getExperienceNote(), actualTypes.contains(UserDocumentType.EXPERIENCE_NOTE), markers);
        validateMissingEvidence(result.getMissingEvidence(), "missingEvidence");
        return result;
    }

    // JSON-05는 설명문 안의 단일 JSON 객체도 허용하되 엄격한 DTO 파싱을 적용한다.
    public CustomizedAnalysisGenerationResult parseCustomizedAnalysis(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            throw parseFailure("JSON-05 empty response");
        }
        try {
            return strictObjectMapper.readValue(extractSingleObject(rawJson), CustomizedAnalysisGenerationResult.class);
        } catch (JsonProcessingException exception) {
            throw parseFailure("JSON-05 parse failed: " + exception.getClass().getSimpleName());
        }
    }

    // interview 추가: JSON-09/11도 기존 AI 응답과 동일하게 알 수 없는 필드를 거부한다.
    public InterviewQuestionGenerationResult parseInterviewQuestions(
            String rawJson,
            String contractName
    ) {
        return parse(rawJson, InterviewQuestionGenerationResult.class, contractName);
    }

    // 응답 전체가 json 코드 블록인 경우만 제거하고 그 밖의 복구는 하지 않는다.
    private <T> T parse(String rawJson, Class<T> type, String contractName) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            throw parseFailure(contractName + " empty response");
        }
        String normalized = rawJson.trim();
        Matcher block = JSON_CODE_BLOCK.matcher(normalized);
        if (block.matches()) {
            normalized = block.group(1).trim();
        }
        try {
            return strictObjectMapper.readValue(normalized, type);
        } catch (JsonProcessingException exception) {
            throw parseFailure(contractName + " parse failed: " + exception.getClass().getSimpleName());
        }
    }

    private String extractSingleObject(String rawJson) {
        String normalized = rawJson.trim();
        Matcher block = JSON_CODE_BLOCK.matcher(normalized);
        if (block.matches()) normalized = block.group(1).trim();
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (quoted) {
                if (escaped) escaped = false;
                else if (character == '\\') escaped = true;
                else if (character == '"') quoted = false;
                continue;
            }
            if (character == '"') { quoted = true; continue; }
            if (character == '{') {
                if (depth++ == 0) start = index;
            } else if (character == '}' && depth > 0 && --depth == 0) {
                objects.add(normalized.substring(start, index + 1));
            }
        }
        if (depth != 0 || objects.size() != 1) {
            throw parseFailure("JSON-05 must contain exactly one JSON object");
        }
        return objects.get(0);
    }

    private void validateResume(CandidateMaterialAnalysisResult.Resume resume, boolean selected, List<AnalysisSourceMarkerParser.SourceMarker> markers) {
        if (!selected) {
            requireAbsent("resume", resume);
            return;
        }
        if (resume == null) throw validation("resume is required");
        requireList("resume.experiences", resume.getExperiences());
        requireList("resume.skills", resume.getSkills());
        requireList("resume.roles", resume.getRoles());
        requireList("resume.results", resume.getResults());
        unique("experienceId", resume.getExperiences(), CandidateMaterialAnalysisResult.Experience::getExperienceId);
        validateItems(resume.getExperiences(), markers, CANDIDATE_TYPES, CandidateMaterialAnalysisResult.Experience::getExperienceId, CandidateMaterialAnalysisResult.Experience::getTitle, CandidateMaterialAnalysisResult.Experience::getSourceRefs);
        for (CandidateMaterialAnalysisResult.Experience item : resume.getExperiences()) {
            requireText("resume.experiences.summary", item.getSummary());
        }
        validateItems(resume.getSkills(), markers, CANDIDATE_TYPES, CandidateMaterialAnalysisResult.Skill::getSkill, CandidateMaterialAnalysisResult.Skill::getUsageContext, CandidateMaterialAnalysisResult.Skill::getSourceRefs);
        validateItems(resume.getRoles(), markers, CANDIDATE_TYPES, CandidateMaterialAnalysisResult.Role::getRole, CandidateMaterialAnalysisResult.Role::getContext, CandidateMaterialAnalysisResult.Role::getSourceRefs);
        for (CandidateMaterialAnalysisResult.Result item : resume.getResults()) {
            requireText("resume.results.result", item.getResult());
            validateRefs(item.getSourceRefs(), markers, CANDIDATE_TYPES, "resume.results.sourceRefs");
        }
    }

    private void validateCoverLetter(CandidateMaterialAnalysisResult.CoverLetter coverLetter, boolean selected, List<AnalysisSourceMarkerParser.SourceMarker> markers) {
        if (!selected) { requireAbsent("coverLetter", coverLetter); return; }
        if (coverLetter == null) throw validation("coverLetter is required");
        validateSummary(coverLetter.getMotivation(), markers, "coverLetter.motivation");
        validateSummary(coverLetter.getValues(), markers, "coverLetter.values");
        requireList("coverLetter.experienceNarratives", coverLetter.getExperienceNarratives());
        coverLetter.getExperienceNarratives().forEach(item -> validateSummary(item, markers, "coverLetter.experienceNarratives"));
        validateSummary(coverLetter.getJobConnection(), markers, "coverLetter.jobConnection");
    }

    private void validatePortfolio(CandidateMaterialAnalysisResult.Portfolio portfolio, boolean selected, List<AnalysisSourceMarkerParser.SourceMarker> markers) {
        if (!selected) { requireAbsent("portfolio", portfolio); return; }
        if (portfolio == null) throw validation("portfolio is required");
        requireList("portfolio.projects", portfolio.getProjects());
        unique("projectId", portfolio.getProjects(), CandidateMaterialAnalysisResult.Project::getProjectId);
        for (CandidateMaterialAnalysisResult.Project project : portfolio.getProjects()) {
            requireText("portfolio.projects.projectId", project.getProjectId());
            requireText("portfolio.projects.projectName", project.getProjectName());
            requireText("portfolio.projects.structure", project.getStructure());
            requireText("portfolio.projects.role", project.getRole());
            requireList("portfolio.projects.contributions", project.getContributions());
            requireList("portfolio.projects.techUsageReasons", project.getTechUsageReasons());
            requireList("portfolio.projects.problemSolving", project.getProblemSolving());
            requireList("portfolio.projects.outputs", project.getOutputs());
            validateRefs(project.getSourceRefs(), markers, CANDIDATE_TYPES, "portfolio.projects.sourceRefs");
        }
    }

    private void validateExperienceNote(CandidateMaterialAnalysisResult.ExperienceNote note, boolean selected, List<AnalysisSourceMarkerParser.SourceMarker> markers) {
        if (!selected) { requireAbsent("experienceNote", note); return; }
        if (note == null) throw validation("experienceNote is required");
        requireList("experienceNote.starCandidates", note.getStarCandidates());
        unique("candidateId", note.getStarCandidates(), CandidateMaterialAnalysisResult.StarCandidate::getCandidateId);
        for (CandidateMaterialAnalysisResult.StarCandidate candidate : note.getStarCandidates()) {
            requireText("experienceNote.starCandidates.candidateId", candidate.getCandidateId());
            requireList("experienceNote.starCandidates.missingParts", candidate.getMissingParts());
            validateRefs(candidate.getSourceRefs(), markers, CANDIDATE_TYPES, "experienceNote.starCandidates.sourceRefs");
        }
    }

    private void validateSummary(CandidateMaterialAnalysisResult.SummaryEvidence value, List<AnalysisSourceMarkerParser.SourceMarker> markers, String field) {
        if (value == null) return;
        requireText(field + ".summary", value.getSummary());
        validateRefs(value.getSourceRefs(), markers, CANDIDATE_TYPES, field + ".sourceRefs");
    }

    private <T> void validateItems(List<T> items, List<AnalysisSourceMarkerParser.SourceMarker> markers, Set<UserDocumentType> allowedTypes,
                                   Function<T, String> id, Function<T, String> text, Function<T, List<SourceReference>> refs) {
        for (T item : items) {
            requireText("item id", id.apply(item));
            requireText("item text", text.apply(item));
            validateRefs(refs.apply(item), markers, allowedTypes, "sourceRefs");
        }
    }

    // sourceRefs는 JSON-03 marker에 있던 동일한 ID·문서유형·페이지·세그먼트만 echo할 수 있다.
    private void validateRefs(List<SourceReference> refs, List<AnalysisSourceMarkerParser.SourceMarker> markers,
                              Set<UserDocumentType> allowedTypes, String field) {
        if (refs == null || refs.isEmpty()) invalidSource(field, "must contain at least one reference");
        for (SourceReference ref : refs) {
            if (ref == null || ref.getExtractionId() == null || ref.getDocumentId() == null || ref.getDocumentType() == null
                    || ref.getEvidenceText() == null || ref.getEvidenceText().isBlank()) {
                invalidSource(field, "missing required reference field");
            }
            if (!allowedTypes.contains(ref.getDocumentType())) invalidSource(field, "documentType is not allowed: " + ref.getDocumentType());
            AnalysisSourceMarkerParser.SourceMarker marker = markers.stream()
                    .filter(value -> value.extractionId().equals(ref.getExtractionId()))
                    .filter(value -> value.documentId().equals(ref.getDocumentId()))
                    .filter(value -> value.documentType() == ref.getDocumentType())
                    .filter(value -> java.util.Objects.equals(value.pageNumber(), ref.getPageNumber()))
                    .filter(value -> java.util.Objects.equals(value.segmentId(), ref.getSegmentId()))
                    .findFirst().orElse(null);
            if (marker == null) invalidSource(field, "unknown marker extractionId=" + ref.getExtractionId() + ", segmentId=" + ref.getSegmentId());
            if (!marker.segmentText().contains(ref.getEvidenceText())) invalidSource(field, "evidenceText is not in segmentId=" + ref.getSegmentId());
        }
    }

    private void validateMissingEvidence(List<?> missingEvidence, String field) {
        for (Object item : missingEvidence) {
            if (item instanceof JobPostingAnalysisResult.MissingEvidence value) {
                requireText(field + ".item", value.getItem()); requireText(field + ".reason", value.getReason());
            } else if (item instanceof CandidateMaterialAnalysisResult.MissingEvidence value) {
                requireText(field + ".item", value.getItem()); requireText(field + ".reason", value.getReason());
            }
        }
    }

    private <T> void unique(String field, List<T> values, Function<T, String> id) {
        Set<String> ids = new HashSet<>();
        for (T value : values) {
            String identifier = id.apply(value);
            requireText(field, identifier);
            if (!ids.add(identifier)) throw validation("duplicate " + field + ": " + identifier);
        }
    }

    @SafeVarargs
    private final <T> List<T> concat(List<T>... values) {
        List<T> result = new ArrayList<>();
        for (List<T> value : values) result.addAll(value);
        return result;
    }

    private void requireList(String field, List<?> values) { if (values == null) throw validation(field + " must not be null"); }
    private void requireText(String field, String value) { if (value == null || value.isBlank()) throw validation(field + " must not be blank"); }
    private void requireAbsent(String field, Object value) { if (value != null) throw validation(field + " exists without selected document type"); }
    private AiProcessingException parseFailure(String message) { return new AiProcessingException(AiCallLogErrorType.RESPONSE_PARSE_FAILED, message); }
    private AiProcessingException validation(String message) { return new AiProcessingException(AiCallLogErrorType.RESPONSE_VALIDATION_FAILED, message); }
    private void invalidSource(String field, String detail) { throw new AiProcessingException(AiCallLogErrorType.SOURCE_REFERENCE_INVALID, field + ": " + detail); }
}
