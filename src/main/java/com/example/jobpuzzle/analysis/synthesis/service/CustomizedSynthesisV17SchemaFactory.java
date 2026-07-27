package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderEvidenceCatalog;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisProviderInput;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** v1.7 compact schema. 11개 requirement에서도 grammar key 수를 두 개의 고정 map으로 제한한다. */
@Component
public class CustomizedSynthesisV17SchemaFactory {
    private final ObjectMapper mapper;

    public CustomizedSynthesisV17SchemaFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode schema(CustomizedSynthesisProviderInput input) {
        if (input == null || input.generationPolicy() == null || input.requirementCatalog().isEmpty()) {
            throw new IllegalArgumentException("JSON-05 v1.7 schema input is incomplete");
        }
        List<String> ids = input.requirementCatalog().stream()
                .map(CustomizedSynthesisProviderInput.RequirementItem::requirementId).toList();
        Map<String, Object> definitions = Map.of("text", Map.of("type", "string"));
        Map<String, Object> decisions = new LinkedHashMap<>();
        Map<String, Object> narratives = new LinkedHashMap<>();
        input.requirementCatalog().forEach(requirement -> {
            List<String> values = new ArrayList<>(List.of("NONE", "INSUFFICIENT"));
            requirement.allowedCandidateEvidenceIds().forEach(evidenceId -> {
                values.add("HIGH::" + evidenceId);
                values.add("MEDIUM::" + evidenceId);
                values.add("LOW::" + evidenceId);
            });
            decisions.put(requirement.requirementId(), Map.of("type", "string", "enum", values));
            narratives.put(requirement.requirementId(), array(ref("text")));
        });
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("readiness", object(Map.of(
                "reason", ref("text"), "limitations", array(ref("text")))));
        properties.put("decisionById", object(decisions));
        properties.put("narrativesById", object(narratives));
        properties.put("primaryQuestion", input.generationPolicy().questionGenerationEnabled()
                ? question(input) : Map.of("type", "null"));
        Map<String, Object> root = new LinkedHashMap<>(object(properties));
        root.put("$defs", definitions);
        JsonNode schema = mapper.valueToTree(root);
        if (countProperties(schema) > 40) {
            throw new IllegalArgumentException("JSON-05 v1.7 schema property budget exceeded");
        }
        return schema;
    }

    private Map<String, Object> question(CustomizedSynthesisProviderInput input) {
        List<String> evidenceIds = input.evidenceCatalog().evidence().stream()
                .map(CustomizedSynthesisProviderEvidenceCatalog.EvidenceItem::evidenceId).distinct().toList();
        if (evidenceIds.isEmpty()) throw new IllegalArgumentException("question evidence catalog is empty");
        return object(Map.of(
                "relatedRequirementId", Map.of("type", "string", "enum", List.of("")),
                "questionType", Map.of("type", "string",
                        "enum", List.of("GENERAL", "COMPANY_FIT", "EXPERIENCE", "PROBLEM_SOLVING", "SKILL")),
                "question", ref("text"), "intent", ref("text"),
                "evaluationFocus", Map.of("type", "string", "enum", List.of(
                        "intentMatch", "specificity", "ownRole", "problemSolving", "resultExpression",
                        "requirementConnection", "guideAlignment", "deliveryClarity")),
                "evidenceId", Map.of("type", "string", "enum", evidenceIds)));
    }

    private int countProperties(JsonNode node) {
        int count = node.has("properties") ? node.get("properties").size() : 0;
        var children = node.elements();
        while (children.hasNext()) count += countProperties(children.next());
        return count;
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
