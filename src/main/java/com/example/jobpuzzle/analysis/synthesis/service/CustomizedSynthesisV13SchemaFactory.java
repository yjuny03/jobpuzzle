package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisSchemaContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON-05 v1.3 provider 전용 DTO와 동일한 Anthropic Structured Output schema를 만든다. */
@Component
public class CustomizedSynthesisV13SchemaFactory {
    private final ObjectMapper mapper;

    public CustomizedSynthesisV13SchemaFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode schema(CustomizedSynthesisSchemaContext context) {
        if (context == null) {
            throw new IllegalArgumentException("JSON-05 v1.3 schema context is incomplete");
        }
        List<String> requirementIds = distinct(context.requirementIds(), "requirementId");
        List<String> postingEvidenceIds = distinct(context.postingEvidenceIds(), "postingEvidenceId");
        List<String> candidateEvidenceIds = distinct(context.candidateEvidenceIds(), "candidateEvidenceId");
        List<String> evidenceIds = new ArrayList<>(postingEvidenceIds);
        evidenceIds.addAll(candidateEvidenceIds);
        evidenceIds = distinct(evidenceIds, "evidenceId");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("readiness", readiness());
        properties.put("requirementMatches", array(requirementMatch(requirementIds, candidateEvidenceIds)));
        properties.put("questions", array(question(requirementIds, evidenceIds)));
        properties.put("tasks", array(task(requirementIds)));
        return mapper.valueToTree(canonical(object(properties)));
    }

    private Map<String, Object> readiness() {
        return object(Map.of(
                "reason", nonEmptyString(),
                "limitations", array(nonEmptyString())));
    }

    private Map<String, Object> requirementMatch(List<String> requirementIds, List<String> candidateEvidenceIds) {
        return object(Map.of(
                "requirementId", identifier(requirementIds),
                "matchLevel", enumeration(List.of("HIGH", "MEDIUM", "LOW", "NONE", "INSUFFICIENT")),
                "reason", nonEmptyString(),
                "missingPoint", nullableString(),
                "candidateEvidence", nullableString(),
                "candidateEvidenceIds", array(identifier(candidateEvidenceIds))));
    }

    private Map<String, Object> question(List<String> requirementIds, List<String> evidenceIds) {
        List<Object> relatedIds = new ArrayList<>(requirementIds);
        relatedIds.add(null);
        return object(Map.of(
                "relatedRequirementId", Map.of("enum", relatedIds),
                "questionType", enumeration(List.of("GENERAL", "COMPANY_FIT", "EXPERIENCE", "PROBLEM_SOLVING", "SKILL")),
                "question", nonEmptyString(),
                "intent", nonEmptyString(),
                "evaluationFocus", nonEmptyArray(enumeration(List.of("intentMatch", "specificity", "ownRole",
                        "problemSolving", "resultExpression", "requirementConnection", "guideAlignment", "deliveryClarity"))),
                "evidenceIds", nonEmptyArray(identifier(evidenceIds))));
    }

    private Map<String, Object> task(List<String> requirementIds) {
        return object(Map.of(
                "relatedRequirementId", identifier(requirementIds),
                "missingPoint", nonEmptyString(),
                "suggestion", nonEmptyString()));
    }

    private List<String> distinct(List<String> values, String field) {
        List<String> distinct = values.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
        if (distinct.size() != values.size()) throw new IllegalArgumentException(field + " values are invalid");
        return distinct;
    }

    private Map<String, Object> object(Map<String, Object> properties) {
        return Map.of("type", "object", "additionalProperties", false,
                "properties", properties, "required", new ArrayList<>(properties.keySet()));
    }

    private Map<String, Object> array(Object items) {
        return Map.of("type", "array", "items", items);
    }

    private Map<String, Object> nonEmptyArray(Object items) {
        // Anthropic Structured Outputs의 제한된 schema 표면만 사용한다.
        // 비어 있지 않아야 한다는 의미 검증은 서버 assembler가 담당한다.
        return array(items);
    }

    private Map<String, Object> nonEmptyString() {
        return Map.of("type", "string");
    }

    private Map<String, Object> nullableString() {
        return Map.of("type", List.of("string", "null"));
    }

    private Map<String, Object> enumeration(List<?> values) {
        return Map.of("enum", values);
    }

    private Map<String, Object> identifier(List<String> values) {
        // 빈 catalog에서는 enum:[] 같은 불능 schema를 만들지 않는다.
        // 모델이 항목을 임의 추가해도 assembler의 정확한 집합 검증이 차단한다.
        return values.isEmpty() ? Map.of("type", "string") : enumeration(values);
    }

    private Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new java.util.TreeMap<>();
            map.forEach((key, child) -> sorted.put(String.valueOf(key), canonical(child)));
            return sorted;
        }
        if (value instanceof List<?> list) return list.stream().map(this::canonical).toList();
        return value;
    }
}
