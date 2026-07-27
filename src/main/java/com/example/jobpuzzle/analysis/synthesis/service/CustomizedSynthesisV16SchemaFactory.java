package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** v1.6 평탄 schema. requirement별 evidence enum을 직접 고정하면서 중첩 grammar를 최소화한다. */
@Component
public class CustomizedSynthesisV16SchemaFactory {
    private static final Pattern TECHNICAL_ID = Pattern.compile("[A-Za-z0-9_-]{1,100}");
    private static final int MAX_REQUIREMENTS = 20;
    private final ObjectMapper mapper;

    public CustomizedSynthesisV16SchemaFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode schema(CustomizedSynthesisProviderInput input) {
        if (input == null || input.generationPolicy() == null || input.evidenceCatalog() == null) {
            throw new IllegalArgumentException("JSON-05 v1.6 schema input is incomplete");
        }
        List<String> ids = input.requirementCatalog().stream()
                .map(CustomizedSynthesisProviderInput.RequirementItem::requirementId).toList();
        if (ids.isEmpty() || ids.size() > MAX_REQUIREMENTS || ids.size() != ids.stream().distinct().count()
                || ids.stream().anyMatch(id -> id == null || !TECHNICAL_ID.matcher(id).matches())) {
            throw new IllegalArgumentException("JSON-05 v1.6 requirement IDs are invalid");
        }
        Set<String> candidateCatalogIds = input.evidenceCatalog().evidence().stream()
                .filter(value -> value.role() == CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE)
                .map(CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem::evidenceId)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, Object> definitions = Map.of(
                "text", Map.of("type", "string"),
                "flag", Map.of("type", "boolean"),
                "matchLevel", Map.of("type", "string",
                        "enum", List.of("HIGH", "MEDIUM", "LOW", "NONE", "INSUFFICIENT")));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("readiness", object(Map.of(
                "reason", ref("text"), "limitations", array(ref("text")))));
        properties.put("matchLevelsById", fixedMap(ids, ignored -> ref("matchLevel")));
        properties.put("matchReasonsById", fixedMap(ids, ignored -> ref("text")));
        properties.put("missingPointsById", fixedMap(ids, ignored -> ref("text")));
        properties.put("candidateEvidenceById", fixedMap(ids, ignored -> ref("text")));
        properties.put("candidateEvidenceIdById", fixedMap(ids, id -> {
            List<String> allowed = new ArrayList<>();
            allowed.add("");
            input.requirementCatalog().stream().filter(value -> value.requirementId().equals(id))
                    .findFirst().orElseThrow().allowedCandidateEvidenceIds().forEach(allowed::add);
            if (allowed.stream().skip(1).anyMatch(value -> value == null
                    || !TECHNICAL_ID.matcher(value).matches() || !candidateCatalogIds.contains(value))) {
                throw new IllegalArgumentException("JSON-05 v1.6 allowed candidate evidence IDs are invalid");
            }
            return Map.of("type", "string", "enum", allowed.stream().distinct().toList());
        }));
        properties.put("primaryQuestion", input.generationPolicy().questionGenerationEnabled()
                ? question(input) : Map.of("type", "null"));
        properties.put("taskApplicableById", fixedMap(ids, ignored -> ref("flag")));
        properties.put("taskMissingPointsById", fixedMap(ids, ignored -> ref("text")));
        properties.put("taskSuggestionsById", fixedMap(ids, ignored -> ref("text")));
        Map<String, Object> root = new LinkedHashMap<>(object(properties));
        root.put("$defs", definitions);
        return mapper.valueToTree(root);
    }

    private Map<String, Object> question(CustomizedSynthesisProviderInput input) {
        List<String> evidenceIds = input.evidenceCatalog().evidence().stream()
                .map(CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem::evidenceId).distinct().toList();
        return object(Map.of(
                "relatedRequirementId", Map.of("type", "string", "enum", List.of("")),
                "questionType", Map.of("type", "string",
                        "enum", List.of("GENERAL", "COMPANY_FIT", "EXPERIENCE", "PROBLEM_SOLVING", "SKILL")),
                "question", ref("text"),
                "intent", ref("text"),
                "evaluationFocus", Map.of("type", "string", "enum", List.of(
                        "intentMatch", "specificity", "ownRole", "problemSolving", "resultExpression",
                        "requirementConnection", "guideAlignment", "deliveryClarity")),
                "evidenceId", evidenceIds.isEmpty() ? ref("text")
                        : Map.of("type", "string", "enum", evidenceIds)));
    }

    private Map<String, Object> fixedMap(List<String> ids,
                                         java.util.function.Function<String, Map<String, Object>> valueSchema) {
        Map<String, Object> properties = new LinkedHashMap<>();
        ids.forEach(id -> properties.put(id, valueSchema.apply(id)));
        return object(properties);
    }

    private Map<String, Object> object(Map<String, Object> properties) {
        return Map.of("type", "object", "additionalProperties", false,
                "properties", properties, "required", new ArrayList<>(properties.keySet()));
    }

    private Map<String, Object> array(Object items) {
        return Map.of("type", "array", "items", items);
    }

    private Map<String, Object> ref(String name) {
        return Map.of("$ref", "#/$defs/" + name);
    }
}
