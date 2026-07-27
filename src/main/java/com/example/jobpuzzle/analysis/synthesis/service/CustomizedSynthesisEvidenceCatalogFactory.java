package com.example.jobpuzzle.analysis.synthesis.service;

import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.rag.dto.RetrievedEvidenceContextDto;
import com.example.jobpuzzle.analysis.synthesis.dto.CustomizedSynthesisEvidenceCatalog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** JSON-01과 고정 retrieval에서 JSON-05 v1.3 권위 evidence catalog를 결정적으로 만든다. */
@Component
public class CustomizedSynthesisEvidenceCatalogFactory {

    public CustomizedSynthesisEvidenceCatalog create(JobPostingAnalysisResult posting,
                                                      RetrievedEvidenceContextDto retrieval) {
        if (posting == null || retrieval == null || retrieval.requirements() == null) {
            throw new IllegalArgumentException("posting and retrieval evidence are required");
        }

        List<RequirementInput> requirements = requirements(posting);
        Map<String, RetrievedEvidenceContextDto.RequirementEvidence> retrievalByRequirement = retrievalByRequirement(retrieval);
        if (!retrievalByRequirement.keySet().equals(requirementIds(requirements))) {
            throw new IllegalArgumentException("JSON-01 and retrieval requirement IDs differ");
        }

        List<CustomizedSynthesisEvidenceCatalog.EvidenceItem> evidence = new ArrayList<>();
        Map<String, String> postingEvidenceIds = new LinkedHashMap<>();
        Map<Long, String> candidateEvidenceIds = new LinkedHashMap<>();
        List<CustomizedSynthesisEvidenceCatalog.RequirementItem> catalogRequirements = new ArrayList<>();

        for (RequirementInput requirement : requirements) {
            List<String> postingIds = postingEvidenceIds(requirement, postingEvidenceIds, evidence);
            RetrievedEvidenceContextDto.RequirementEvidence retrieved = retrievalByRequirement.get(requirement.id());
            if (retrieved.requirementType() != requirement.type() || !requirement.text().equals(retrieved.requirementText())) {
                throw new IllegalArgumentException("retrieval requirement metadata differs from JSON-01");
            }
            List<String> candidateIds = candidateEvidenceIds(retrieved, candidateEvidenceIds, evidence);
            catalogRequirements.add(new CustomizedSynthesisEvidenceCatalog.RequirementItem(
                    requirement.id(), requirement.type(), requirement.text(), List.copyOf(postingIds), List.copyOf(candidateIds)));
        }
        return new CustomizedSynthesisEvidenceCatalog(List.copyOf(catalogRequirements), List.copyOf(evidence));
    }

    private List<RequirementInput> requirements(JobPostingAnalysisResult posting) {
        List<RequirementInput> values = new ArrayList<>();
        addRequirements(values, posting.getRequirements(), RequirementType.REQUIRED);
        addRequirements(values, posting.getPreferred(), RequirementType.PREFERRED);
        if (values.isEmpty() || requirementIds(values).size() != values.size()) {
            throw new IllegalArgumentException("JSON-01 requirements are invalid");
        }
        return values;
    }

    private void addRequirements(List<RequirementInput> target, List<JobPostingAnalysisResult.Requirement> source,
                                 RequirementType type) {
        if (source == null) return;
        for (JobPostingAnalysisResult.Requirement value : source) {
            if (value == null || blank(value.getRequirementId()) || blank(value.getText())
                    || value.getSourceRefs() == null || value.getSourceRefs().isEmpty()) {
                throw new IllegalArgumentException("JSON-01 requirement source is invalid");
            }
            target.add(new RequirementInput(value.getRequirementId(), type, value.getText(), value.getSourceRefs()));
        }
    }

    private Map<String, RetrievedEvidenceContextDto.RequirementEvidence> retrievalByRequirement(
            RetrievedEvidenceContextDto retrieval) {
        Map<String, RetrievedEvidenceContextDto.RequirementEvidence> values = new HashMap<>();
        for (RetrievedEvidenceContextDto.RequirementEvidence item : retrieval.requirements()) {
            if (item == null || blank(item.requirementId()) || item.requirementType() == null
                    || blank(item.requirementText()) || item.chunks() == null
                    || values.put(item.requirementId(), item) != null) {
                throw new IllegalArgumentException("retrieval evidence is invalid");
            }
        }
        return values;
    }

    private Set<String> requirementIds(List<RequirementInput> requirements) {
        Set<String> ids = new HashSet<>();
        requirements.forEach(value -> ids.add(value.id()));
        return ids;
    }

    private List<String> postingEvidenceIds(RequirementInput requirement, Map<String, String> ids,
                                            List<CustomizedSynthesisEvidenceCatalog.EvidenceItem> evidence) {
        List<String> values = new ArrayList<>();
        for (SourceReference source : requirement.sourceRefs()) {
            requireSource(source, "posting source");
            String key = sourceKey(source);
            String evidenceId = ids.get(key);
            if (evidenceId == null) {
                evidenceId = "posting-" + (ids.size() + 1);
                ids.put(key, evidenceId);
                evidence.add(new CustomizedSynthesisEvidenceCatalog.EvidenceItem(evidenceId,
                        CustomizedSynthesisEvidenceCatalog.EvidenceRole.POSTING, copy(source)));
            }
            values.add(evidenceId);
        }
        return values.stream().distinct().toList();
    }

    private List<String> candidateEvidenceIds(RetrievedEvidenceContextDto.RequirementEvidence requirement,
                                              Map<Long, String> ids,
                                              List<CustomizedSynthesisEvidenceCatalog.EvidenceItem> evidence) {
        List<String> values = new ArrayList<>();
        for (RetrievedEvidenceContextDto.RetrievedChunk chunk : requirement.chunks()) {
            if (chunk == null || chunk.chunkId() == null || chunk.extractionId() == null || chunk.documentId() == null
                    || chunk.documentType() == null || chunk.content() == null) {
                throw new IllegalArgumentException("retrieval chunk is invalid");
            }
            SourceReference source = SourceReference.builder().extractionId(chunk.extractionId()).documentId(chunk.documentId())
                    .documentType(chunk.documentType()).pageNumber(chunk.pageStart()).segmentId(null).evidenceText(chunk.content()).build();
            String evidenceId = ids.get(chunk.chunkId());
            if (evidenceId == null) {
                evidenceId = "candidate-chunk-" + chunk.chunkId();
                ids.put(chunk.chunkId(), evidenceId);
                evidence.add(new CustomizedSynthesisEvidenceCatalog.EvidenceItem(evidenceId,
                        CustomizedSynthesisEvidenceCatalog.EvidenceRole.CANDIDATE, source));
            }
            values.add(evidenceId);
        }
        return values.stream().distinct().toList();
    }

    private void requireSource(SourceReference source, String field) {
        if (source == null || source.getExtractionId() == null || source.getDocumentId() == null
                || source.getDocumentType() == null || source.getPageNumber() == null || blank(source.getEvidenceText())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
    }

    private SourceReference copy(SourceReference source) {
        return SourceReference.builder().extractionId(source.getExtractionId()).documentId(source.getDocumentId())
                .documentType(source.getDocumentType()).pageNumber(source.getPageNumber()).segmentId(source.getSegmentId())
                .evidenceText(source.getEvidenceText()).build();
    }

    private String sourceKey(SourceReference source) {
        return source.getExtractionId() + "|" + source.getDocumentId() + "|" + source.getDocumentType() + "|"
                + source.getPageNumber() + "|" + Objects.toString(source.getSegmentId(), "") + "|" + source.getEvidenceText();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record RequirementInput(String id, RequirementType type, String text, List<SourceReference> sourceRefs) {
    }
}
