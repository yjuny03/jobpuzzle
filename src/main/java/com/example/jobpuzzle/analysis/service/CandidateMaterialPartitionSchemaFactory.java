package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

// JSON-02의 partition 경계를 provider constrained decoding으로 강제한다.
@Component
public class CandidateMaterialPartitionSchemaFactory {
    public static final String SCHEMA_VERSION = "json02-partition-schema-v4";
    public static final String CONTRACT_VERSION = "candidate-material-contract-v3";
    private final ObjectMapper mapper;
    public CandidateMaterialPartitionSchemaFactory(ObjectMapper mapper) { this.mapper = mapper; }

    public JsonNode schema(UserDocumentType type) {
        if (!Set.of(UserDocumentType.RESUME, UserDocumentType.COVER_LETTER, UserDocumentType.PORTFOLIO, UserDocumentType.EXPERIENCE_NOTE).contains(type))
            throw new IllegalArgumentException("unsupported candidate document type: " + type);
        Map<String,Object> properties = new LinkedHashMap<>();
        properties.put("availableDocumentTypes", Map.of("type","array","items",Map.of("enum",List.of(type.name()))));
        properties.put("resume", type == UserDocumentType.RESUME ? resume() : Map.of("type","null"));
        properties.put("coverLetter", type == UserDocumentType.COVER_LETTER ? coverLetter() : Map.of("type","null"));
        properties.put("portfolio", type == UserDocumentType.PORTFOLIO ? portfolio() : Map.of("type","null"));
        properties.put("experienceNote", type == UserDocumentType.EXPERIENCE_NOTE ? experienceNote() : Map.of("type","null"));
        properties.put("missingEvidence", array(missingEvidence()));
        Map<String,Object> root = new LinkedHashMap<>(object(properties));
        root.put("$defs", Map.of("sourceReference", refDefinition()));
        JsonNode schema = mapper.valueToTree(canonical(root));
        restrictSourceReferenceDocumentType(schema, type.name());
        return schema;
    }

    public String fingerprintMaterial(UserDocumentType type) { return CONTRACT_VERSION + "|" + SCHEMA_VERSION + "|" + hash(schema(type).toString()); }
    // 참조용 식별자는 사실이 아니므로 모델에게 생성시키지 않고 서버가 partition 결과에 결정적으로 부여한다.
    private Map<String,Object> resume() { return object(Map.of("experiences",array(object(withReferences(Map.of("title",str(),"period",nullable("string"),"summary",str())))),"skills",array(object(withReferences(Map.of("skill",str(),"usageContext",str())))),"roles",array(object(withReferences(Map.of("role",str(),"context",str())))),"results",array(object(withReferences(Map.of("result",str())))))); }
    private Map<String,Object> coverLetter() { return object(Map.of("motivation",nullableObject(summaryEvidence()),"values",nullableObject(summaryEvidence()),"jobConnection",nullableObject(summaryEvidence()),"experienceNarratives",array(summaryEvidence()))); }
    private Map<String,Object> portfolio() {
        Map<String,Object> project = withReferences(Map.of("projectName",str(),"structure",str(),
                "role",str(),"contributions",array(str()),"techUsageReasons",array(str()),"problemSolving",array(str()),
                "outputs",array(str())));
        return object(Map.of("projects", array(object(project))));
    }
    private Map<String,Object> experienceNote() {
        Map<String,Object> candidate = withReferences(Map.of("situation",nullable("string"),
                "task",nullable("string"),"action",nullable("string"),"result",nullable("string"),
                "missingParts",array(Map.of("enum",List.of("SITUATION","TASK","ACTION","RESULT")))));
        return object(Map.of("starCandidates", array(object(candidate))));
    }
    private Map<String,Object> summaryEvidence() { return object(withReferences(Map.of("summary",str()))); }
    private Map<String,Object> missingEvidence() { return object(Map.of("item",str(),"reason",str())); }
    private Map<String,Object> refDefinition() { return object(Map.of("extractionId",Map.of("type","integer"),"documentId",Map.of("type","integer"),"documentType",Map.of("enum",List.of("RESUME","COVER_LETTER","PORTFOLIO","EXPERIENCE_NOTE")),"pageNumber",Map.of("type","integer"),"segmentId",str())); }
    private Map<String,Object> ref() { return Map.of("$ref", "#/$defs/sourceReference"); }
    private Map<String,Object> withReferences(Map<String,Object> facts) { Map<String,Object> fields = new LinkedHashMap<>(facts); fields.put("sourceRef", ref()); fields.put("additionalSourceRefs", array(ref())); return fields; }
    private Map<String,Object> object(Map<String,Object> properties) { return Map.of("type","object","additionalProperties",false,"properties",properties,"required",new ArrayList<>(properties.keySet())); }
    private Map<String,Object> array(Object items) { return Map.of("type","array","items",items); }
    // Anthropic Structured Outputs subset은 minLength 같은 cardinality 제약을 지원하지 않을 수 있다.
    // 빈 문자열·잘못된 sourceRef는 기존 DTO/source-reference validator가 canonical 결과에서 차단한다.
    private Map<String,Object> str() { return Map.of("type","string"); }
    private Map<String,Object> nullable(String type) { return Map.of("type",List.of(type,"null")); }
    private Map<String,Object> nullableObject(Map<String,Object> schema) { Map<String,Object> copy=new LinkedHashMap<>(schema); copy.put("type",List.of("object","null")); return copy; }
    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private void restrictSourceReferenceDocumentType(JsonNode node, String documentType) {
        if (node instanceof ObjectNode object) {
            JsonNode properties = object.get("properties");
            if (properties instanceof ObjectNode fields && fields.has("documentType")) {
                ((ObjectNode) fields.get("documentType")).set("enum", mapper.valueToTree(List.of(documentType)));
            }
            object.elements().forEachRemaining(child -> restrictSourceReferenceDocumentType(child, documentType));
        } else if (node instanceof ArrayNode array) array.elements().forEachRemaining(child -> restrictSourceReferenceDocumentType(child, documentType));
    }
    private Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String,Object> sorted = new TreeMap<>();
            map.forEach((key, child) -> sorted.put(String.valueOf(key), canonical(child)));
            return sorted;
        }
        if (value instanceof List<?> list) return list.stream().map(this::canonical).toList();
        return value;
    }
}
