package com.example.jobpuzzle.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// JSON-05의 provider 응답만 제약한다. 이후 DTO·validator·QuestionSet 저장 계약은 변경하지 않는다.
@Component
public class CustomizedSynthesisSchemaFactory {
    private final ObjectMapper mapper;
    public CustomizedSynthesisSchemaFactory(ObjectMapper mapper) { this.mapper = mapper; }

    public JsonNode schema() { return schema(List.of(), List.of()); }

    // JSON-05 rendered prompt의 retrieval projection은 requirement 목록을 반드시 포함한다.
    // 실행별 목록을 schema에 반영해 모델이 모르는 ID를 만들거나 너무 적은 match만 반환하는 것을 Provider 단계에서 막는다.
    public JsonNode schemaForPrompt(String prompt) {
        try {
            String opening = "<RETRIEVED_EVIDENCE>";
            String closing = "</RETRIEVED_EVIDENCE>";
            int start = prompt.indexOf(opening);
            int end = prompt.indexOf(closing, start + opening.length());
            if (start < 0 || end < 0) return schema();
            JsonNode requirements = mapper.readTree(prompt.substring(start + opening.length(), end)).path("requirements");
            if (!requirements.isArray()) return schema();
            List<String> ids = new ArrayList<>();
            for (JsonNode requirement : requirements) {
                String id = requirement.path("requirementId").asText();
                if (!id.isBlank() && !ids.contains(id)) ids.add(id);
            }
            List<String> evidenceIds = new ArrayList<>();
            JsonNode evidence = mapper.readTree(prompt.substring(start + opening.length(), end)).path("candidateEvidence");
            if (evidence.isArray()) for (JsonNode value : evidence) {
                String id = value.path("evidenceId").asText();
                if (!id.isBlank() && !evidenceIds.contains(id)) evidenceIds.add(id);
            }
            return schema(ids, evidenceIds);
        } catch (Exception ignored) {
            // prompt renderer가 이미 JSON 직렬화를 보장한다. 예외 시에는 기존 static schema로 안전하게 후퇴한다.
            return schema();
        }
    }

    public JsonNode schema(List<String> requirementIds) { return schema(requirementIds, List.of()); }

    public JsonNode schema(List<String> requirementIds, List<String> candidateEvidenceIds) {
        List<String> ids = requirementIds == null ? List.of() : requirementIds.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).distinct().toList();
        List<String> evidenceIds = candidateEvidenceIds == null ? List.of() : candidateEvidenceIds.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).distinct().toList();
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("readiness", readiness());
        properties.put("evidencedRequirementMatches", array(evidencedRequirementMatch(ids, evidenceIds)));
        properties.put("nonEvidencedRequirementMatches", array(nonEvidencedRequirementMatch(ids)));
        properties.put("questions", array(question()));
        properties.put("tasks", array(task()));
        Map<String, Object> root = new LinkedHashMap<>(object(properties));
        // Anthropic Structured Outputs는 root anyOf와 $defs를 함께 지원하지 않는다.
        // 실행별 requirementId enum은 provider 단계에서 유지하고, 두 배열의 합계 coverage는 validator가 검증한다.
        // 반복되는 근거 구조를 한 번만 정의해 grammar 크기를 제한한다.
        root.put("$defs", Map.of("sourceReference", sourceReference()));
        return mapper.valueToTree(canonical(root));
    }

    // readiness.reason은 결과 화면의 사용자 안내 문구이므로 빈 문자열을 provider schema 단계에서 차단한다.
    // validator가 서버 데이터와 비교하기 전에 schema만으로 막을 수 있는 빈 값·빈 배열을 모두 제한한다.
    // match/task/question 관계, requirement coverage, source identity는 입력 데이터가 있어야 검증할 수 있으므로 validator에 남긴다.
    private Map<String, Object> readiness() { return object(Map.of("status", enumeration("SUFFICIENT", "PARTIAL", "GUIDE_LACK", "POSTING_LACK", "CANDIDATE_LACK"), "canGenerateQuestions", Map.of("type", "boolean"), "reason", nonEmptyString(), "limitations", array(nonEmptyString()))); }
    private Map<String, Object> evidencedRequirementMatch(List<String> ids, List<String> evidenceIds) { return object(Map.of("matchId", nonEmptyString(), "requirementId", requirementId(ids), "requirementType", enumeration("REQUIRED", "PREFERRED"), "requirement", nonEmptyString(), "postingSourceRefs", nonEmptyArray(sourceReferenceRef()), "candidateEvidence", nonEmptyString(), "candidateEvidenceIds", nonEmptyArray(evidenceId(evidenceIds)), "matchLevel", enumeration("HIGH", "MEDIUM", "LOW"), "reason", nonEmptyString(), "missingPoint", nullableString())); }
    private Map<String, Object> nonEvidencedRequirementMatch(List<String> ids) { return object(Map.of("matchId", nonEmptyString(), "requirementId", requirementId(ids), "requirementType", enumeration("REQUIRED", "PREFERRED"), "requirement", nonEmptyString(), "postingSourceRefs", nonEmptyArray(sourceReferenceRef()), "matchLevel", enumeration("NONE", "INSUFFICIENT"), "reason", nonEmptyString(), "missingPoint", nonEmptyString())); }
    private Map<String, Object> question() { return object(Map.of("questionId", nonEmptyString(), "questionType", enumeration("GENERAL", "COMPANY_FIT", "EXPERIENCE", "PROBLEM_SOLVING", "SKILL"), "question", nonEmptyString(), "intent", nonEmptyString(), "evaluationFocus", nonEmptyArray(enumeration("intentMatch", "specificity", "ownRole", "problemSolving", "resultExpression", "requirementConnection", "guideAlignment", "deliveryClarity")), "relatedMatchId", nullableString(), "relatedRequirementId", nullableString(), "sourceRefs", nonEmptyArray(sourceReferenceRef()), "reviewStatus", enumeration("PASS"), "reviewNote", nullableString())); }
    private Map<String, Object> task() { return object(Map.of("taskId", nonEmptyString(), "relatedMatchId", nonEmptyString(), "relatedRequirementId", nonEmptyString(), "matchLevel", enumeration("MEDIUM", "LOW", "NONE", "INSUFFICIENT"), "missingPoint", nonEmptyString(), "suggestion", nonEmptyString())); }
    private Map<String, Object> sourceReference() { return object(Map.of("extractionId", Map.of("type", "integer"), "documentId", Map.of("type", "integer"), "documentType", enumeration("JOB_POSTING", "COMPANY_INFO", "RESUME", "COVER_LETTER", "PORTFOLIO", "EXPERIENCE_NOTE"), "pageNumber", nullableInteger(), "segmentId", nullableString(), "evidenceText", nonEmptyString())); }
    private Map<String, Object> sourceReferenceRef() { return Map.of("$ref", "#/$defs/sourceReference"); }
    private Map<String, Object> object(Map<String, Object> properties) { return Map.of("type", "object", "additionalProperties", false, "properties", properties, "required", new ArrayList<>(properties.keySet())); }
    private Map<String, Object> array(Object items) { return Map.of("type", "array", "items", items); }
    private Object requirementId(List<String> ids) { return ids.isEmpty() ? nonEmptyString() : Map.of("enum", ids); }
    private Object evidenceId(List<String> ids) { return ids.isEmpty() ? nonEmptyString() : Map.of("enum", ids); }
    private Map<String, Object> nonEmptyArray(Object items) { return Map.of("type", "array", "items", items, "minItems", 1); }
    private Map<String, Object> string() { return Map.of("type", "string"); }
    private Map<String, Object> nonEmptyString() { return Map.of("type", "string", "minLength", 1); }
    private Map<String, Object> nullableString() { return Map.of("type", List.of("string", "null")); }
    private Map<String, Object> nullableInteger() { return Map.of("type", List.of("integer", "null")); }
    private Map<String, Object> enumeration(String... values) { return Map.of("enum", List.of(values)); }
    private Object canonical(Object value) { if (value instanceof Map<?, ?> map) { Map<String, Object> sorted = new java.util.TreeMap<>(); map.forEach((key, child) -> sorted.put(String.valueOf(key), canonical(child))); return sorted; } if (value instanceof List<?> list) return list.stream().map(this::canonical).toList(); return value; }
}
