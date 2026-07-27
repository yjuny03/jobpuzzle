package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderInput;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** v1.5 필수 슬롯 schema. 지원하지 않는 길이·조건부·union 제약을 사용하지 않는다. */
@Component
public class CustomizedSynthesisV15SchemaFactory {
    private static final Pattern TECHNICAL_ID = Pattern.compile("[A-Za-z0-9_-]{1,100}");
    private static final int MAX_REQUIREMENTS = 20;
    private final ObjectMapper mapper;

    public CustomizedSynthesisV15SchemaFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode schema(CustomizedSynthesisProviderInput input) {
        if (input == null || input.generationPolicy() == null || input.evidenceCatalog() == null) {
            throw new IllegalArgumentException("JSON-05 v1.5 schema input is incomplete");
        }
        List<String> requirementIds = distinctTechnicalIds(
                input.requirementCatalog().stream()
                        .map(CustomizedSynthesisProviderInput.RequirementItem::requirementId).toList(),
                "requirementId");
        if (requirementIds.size() > MAX_REQUIREMENTS) {
            throw new IllegalArgumentException("JSON-05 v1.5 requirement count exceeds schema limit");
        }
        List<String> postingIds = distinctTechnicalIds(input.evidenceCatalog().evidence().stream()
                .filter(value -> value.role() == CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING)
                .map(CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem::evidenceId).toList(), "postingEvidenceId");
        List<String> candidateIds = distinctTechnicalIds(input.evidenceCatalog().evidence().stream()
                .filter(value -> value.role() == CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE)
                .map(CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem::evidenceId).toList(), "candidateEvidenceId");
        List<String> allEvidenceIds = new ArrayList<>(postingIds);
        allEvidenceIds.addAll(candidateIds);
        distinctTechnicalIds(allEvidenceIds, "evidenceId");

        Map<String, Object> definitions = new LinkedHashMap<>();
        definitions.put("matchSlot", matchSlot(candidateIds));
        definitions.put("taskSlot", taskSlot());
        Map<String, Object> top = new LinkedHashMap<>();
        top.put("readiness", readiness());
        top.put("requirementMatchesById", requiredSlots(requirementIds, reference("matchSlot")));
        top.put("primaryQuestion", input.generationPolicy().questionGenerationEnabled()
                ? questionSlot(requirementIds, allEvidenceIds) : Map.of("type", "null"));
        top.put("tasksByRequirementId", requiredSlots(requirementIds, reference("taskSlot")));
        Map<String, Object> root = new LinkedHashMap<>(object(top));
        root.put("$defs", definitions);
        JsonNode schema = mapper.valueToTree(canonical(root));
        validateComplexity(schema);
        return schema;
    }

    private Map<String, Object> readiness() {
        return object(Map.of("reason", string(), "limitations", array(string())));
    }

    private Map<String, Object> matchSlot(List<String> candidateIds) {
        return object(Map.of(
                "matchLevel", enumeration(List.of("HIGH", "MEDIUM", "LOW", "NONE", "INSUFFICIENT")),
                "reason", string(),
                "missingPoint", string(),
                "candidateEvidence", string(),
                "candidateEvidenceIds", array(identifier(candidateIds))));
    }

    private Map<String, Object> questionSlot(List<String> requirementIds, List<String> evidenceIds) {
        List<String> related = new ArrayList<>();
        related.add("");
        related.addAll(requirementIds);
        return object(Map.of(
                "relatedRequirementId", enumeration(related),
                "questionType", enumeration(List.of("GENERAL", "COMPANY_FIT", "EXPERIENCE", "PROBLEM_SOLVING", "SKILL")),
                "question", string(),
                "intent", string(),
                "evaluationFocus", array(enumeration(List.of("intentMatch", "specificity", "ownRole",
                        "problemSolving", "resultExpression", "requirementConnection", "guideAlignment", "deliveryClarity"))),
                "evidenceIds", array(identifier(evidenceIds))));
    }

    private Map<String, Object> taskSlot() {
        return object(Map.of(
                "applicable", Map.of("type", "boolean"),
                "missingPoint", string(),
                "suggestion", string()));
    }

    private Map<String, Object> requiredSlots(List<String> ids, Map<String, Object> slotSchema) {
        Map<String, Object> properties = new LinkedHashMap<>();
        ids.forEach(id -> properties.put(id, slotSchema));
        return object(properties);
    }

    private Map<String, Object> reference(String definition) {
        return Map.of("$ref", "#/$defs/" + definition);
    }

    private List<String> distinctTechnicalIds(List<String> values, String field) {
        List<String> distinct = values.stream().distinct().toList();
        if (distinct.size() != values.size()
                || distinct.stream().anyMatch(value -> value == null || !TECHNICAL_ID.matcher(value).matches())) {
            throw new IllegalArgumentException(field + " values are invalid");
        }
        return distinct;
    }

    private Map<String, Object> object(Map<String, Object> properties) {
        return Map.of("type", "object", "additionalProperties", false,
                "properties", properties, "required", new ArrayList<>(properties.keySet()));
    }

    private Map<String, Object> array(Object items) {
        return Map.of("type", "array", "items", items);
    }

    private Map<String, Object> string() {
        return Map.of("type", "string");
    }

    private Map<String, Object> identifier(List<String> values) {
        return values.isEmpty() ? string() : enumeration(values);
    }

    private Map<String, Object> enumeration(List<?> values) {
        return Map.of("enum", values);
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

    private void validateComplexity(JsonNode schema) {
        Complexity complexity = complexity(schema);
        if (complexity.unionTypes() > 16 || complexity.optionalProperties() > 24) {
            throw new IllegalArgumentException("JSON-05 v1.5 schema exceeds Anthropic complexity limits");
        }
    }

    private Complexity complexity(JsonNode node) {
        int unions = node.has("anyOf") ? 1 : 0;
        JsonNode type = node.get("type");
        if (type != null && type.isArray()) unions++;
        int optional = 0;
        JsonNode properties = node.get("properties");
        if (properties != null && properties.isObject()) {
            java.util.Set<String> required = new java.util.HashSet<>();
            JsonNode requiredNode = node.get("required");
            if (requiredNode != null) requiredNode.forEach(value -> required.add(value.asText()));
            var names = properties.fieldNames();
            while (names.hasNext()) if (!required.contains(names.next())) optional++;
        }
        var children = node.elements();
        while (children.hasNext()) {
            Complexity child = complexity(children.next());
            unions += child.unionTypes();
            optional += child.optionalProperties();
        }
        return new Complexity(unions, optional);
    }

    private record Complexity(int unionTypes, int optionalProperties) {
    }
}
