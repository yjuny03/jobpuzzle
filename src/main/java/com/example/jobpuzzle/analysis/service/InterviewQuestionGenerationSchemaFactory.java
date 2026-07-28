package com.example.jobpuzzle.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-09(약점 보완)와 JSON-11(기본 질문)의 Claude Structured Output 계약.
 */
@Component
public class InterviewQuestionGenerationSchemaFactory {

    private final ObjectMapper mapper;

    public InterviewQuestionGenerationSchemaFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode schema() {
        Map<String, Object> rootProperties = new LinkedHashMap<>();
        rootProperties.put("questions", array(question()));
        return mapper.valueToTree(object(rootProperties));
    }

    private Map<String, Object> question() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("questionId", nonEmptyString());
        properties.put("questionType", enumeration(
                "SELF_INTRO", "MOTIVATION", "STRENGTH_WEAKNESS",
                "FAILURE_CONFLICT", "JOB_GENERAL", "WEAKNESS_FOLLOWUP"
        ));
        properties.put("question", nonEmptyString());
        properties.put("intent", nonEmptyString());
        properties.put("evaluationFocus", nonEmptyArray(enumeration(
                "intentMatch", "specificity", "ownRole", "problemSolving",
                "resultExpression", "requirementConnection", "guideAlignment", "deliveryClarity"
        )));
        properties.put("originEvaluationId", nullableInteger());
        properties.put("targetWeaknessTag", nullableString());
        properties.put("targetDimension", nullableString());
        properties.put("reviewStatus", enumeration("PASS"));
        properties.put("reviewNote", nullableString());
        return object(properties);
    }

    private Map<String, Object> object(Map<String, Object> properties) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("type", "object");
        value.put("additionalProperties", false);
        value.put("properties", properties);
        value.put("required", new ArrayList<>(properties.keySet()));
        return value;
    }

    private Map<String, Object> array(Object items) {
        return Map.of("type", "array", "items", items);
    }

    private Map<String, Object> nonEmptyArray(Object items) {
        return Map.of("type", "array", "items", items, "minItems", 1);
    }

    private Map<String, Object> nonEmptyString() {
        return Map.of("type", "string", "minLength", 1);
    }

    private Map<String, Object> nullableString() {
        return Map.of("type", List.of("string", "null"));
    }

    private Map<String, Object> nullableInteger() {
        return Map.of("type", List.of("integer", "null"));
    }

    private Map<String, Object> enumeration(String... values) {
        return Map.of("enum", List.of(values));
    }
}
