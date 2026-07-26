package com.example.jobpuzzle.analysis.rag.service;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.analysis.entity.JobPostingAnalysis;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchRequest;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchResult;
import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalChunk;
import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalResult;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalCorpusType;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalStatus;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalChunkRepository;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalResultRepository;
import com.example.jobpuzzle.analysis.rag.search.VectorSearchPort;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import com.example.jobpuzzle.analysis.repository.JobPostingAnalysisRepository;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** JSON-01 requirement마다 candidate 검색 결과를 snapshot 기준으로 저장·재사용한다. */
@Service
@RequiredArgsConstructor
public class RequirementRetrievalService {

    private static final Set<UserDocumentType> CANDIDATE_DOCUMENT_TYPES = EnumSet.of(
            UserDocumentType.RESUME, UserDocumentType.COVER_LETTER,
            UserDocumentType.PORTFOLIO, UserDocumentType.EXPERIENCE_NOTE);

    private final AnalysisInputSnapshotRepository snapshotRepository;
    private final JobPostingAnalysisRepository jobPostingAnalysisRepository;
    private final AnalysisMaterialChunkRepository materialChunkRepository;
    private final RequirementRetrievalResultRepository retrievalResultRepository;
    private final RequirementRetrievalChunkRepository retrievalChunkRepository;
    private final VectorSearchPort vectorSearchPort;

    // snapshot lock 안에서 JSON-01 requirement별 retrieval 계약을 확인하고 정상 결과만 재사용한다.
    @Transactional
    public List<RequirementRetrievalResult> getOrCreateCandidateRetrievals(Long userId, Long snapshotId, int topK) {
        if (userId == null || snapshotId == null || topK < 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "userId, snapshotId and positive topK are required");
        }
        // 동일 snapshot의 retrieval 동시 생성으로 인한 unique 충돌과 중복 검색을 막는다.
        AnalysisInputSnapshot snapshot = snapshotRepository.findWithLockBySnapshotId(snapshotId)
                .orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));
        if (!userId.equals(snapshot.getUser().getUserId())) {
            throw new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND);
        }
        JobPostingAnalysis posting = validJobPosting(snapshotId);
        List<RequirementInput> requirements = requirements(posting);
        String provider = required(vectorSearchPort.getEmbeddingProviderName(), "embedding provider");
        String model = required(vectorSearchPort.getEmbeddingModelName(), "embedding model");

        List<RequirementRetrievalResult> results = new ArrayList<>();
        for (RequirementInput requirement : requirements) {
            String queryText = queryText(snapshot, requirement.text());
            String queryHash = queryHash(queryText, provider, model, topK, CANDIDATE_DOCUMENT_TYPES,
                    com.example.jobpuzzle.analysis.service.AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION);
            RequirementRetrievalResult existing = retrievalResultRepository
                    .findBySnapshot_SnapshotIdAndRequirementIdAndCorpusType(snapshotId, requirement.id(), RetrievalCorpusType.CANDIDATE)
                    .orElse(null);
            if (isReusable(existing, snapshot, userId, requirement, queryText, queryHash, provider, model, topK)) {
                results.add(existing);
                continue;
            }
            if (existing != null) deleteExisting(existing);
            List<AnalysisMaterialChunkSearchResult> searched = vectorSearchPort.search(new AnalysisMaterialChunkSearchRequest(
                    userId, snapshotId, queryText, topK, CANDIDATE_DOCUMENT_TYPES,
                    com.example.jobpuzzle.analysis.service.AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION));
            List<ResolvedSearchResult> resolved = resolveAndValidateSearchResults(searched, userId, snapshotId, topK);
            RetrievalStatus status = resolved.isEmpty() ? RetrievalStatus.EMPTY : RetrievalStatus.COMPLETED;
            RequirementRetrievalResult created = retrievalResultRepository.save(RequirementRetrievalResult.create(
                    snapshot, userId, requirement.id(), requirement.type(), requirement.text(), RetrievalCorpusType.CANDIDATE,
                    queryText, queryHash, provider, model, topK, status));
            if (!resolved.isEmpty()) {
                retrievalChunkRepository.saveAll(resolved.stream().map(value -> RequirementRetrievalChunk.create(
                        created, value.materialChunk(), value.searchResult().rank(), value.searchResult().score())).toList());
            }
            results.add(created);
        }
        return results;
    }

    // JSON-01의 AI 성공·valid 상태를 확인해 실패하거나 불완전한 requirement를 검색하지 않게 한다.
    private JobPostingAnalysis validJobPosting(Long snapshotId) {
        JobPostingAnalysis posting = jobPostingAnalysisRepository.findBySnapshot_SnapshotId(snapshotId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_CASE_NOT_READY, "JSON-01 result is missing"));
        AiCallLog log = posting.getAiCallLog();
        if (log == null || log.getStatus() != AiCallLogStatus.SUCCEEDED || !Boolean.TRUE.equals(log.getValid())) {
            throw new CustomException(ErrorCode.ANALYSIS_CASE_NOT_READY, "JSON-01 is not succeeded and valid");
        }
        return posting;
    }

    // REQUIRED 목록 뒤 PREFERRED 목록을 원래 JSON 배열 순서대로 결합하고 ID 중복을 차단한다.
    private List<RequirementInput> requirements(JobPostingAnalysis posting) {
        List<RequirementInput> values = new ArrayList<>();
        addRequirements(values, posting.getRequirements(), RequirementType.REQUIRED);
        addRequirements(values, posting.getPreferred(), RequirementType.PREFERRED);
        Set<String> ids = new HashSet<>();
        for (RequirementInput value : values) {
            if (blank(value.id()) || blank(value.text()) || !ids.add(value.id())) {
                throw new CustomException(ErrorCode.INVALID_REQUEST, "JSON-01 requirement ID and text must be unique and non-blank");
            }
        }
        return values;
    }

    private void addRequirements(List<RequirementInput> target, List<JobPostingAnalysis.Requirement> source, RequirementType type) {
        if (source == null) return;
        source.forEach(value -> target.add(new RequirementInput(value.getRequirementId(), type, value.getText())));
    }

    // requirement와 snapshot 직무 기준을 고정된 순서로 조합해 재현 가능한 검색어를 만든다.
    private String queryText(AnalysisInputSnapshot snapshot, String requirementText) {
        List<String> values = new ArrayList<>();
        values.add(requirementText.trim());
        String subCategory = snapshot.getJobCategory().getSubCategory();
        String mainCategory = snapshot.getJobCategory().getMainCategory();
        if (!blank(subCategory)) values.add(subCategory.trim());
        else if (!blank(mainCategory)) values.add(mainCategory.trim());
        if (snapshot.getJobCategory().getCareerLevel() != null) values.add(snapshot.getJobCategory().getCareerLevel().name());
        return String.join(" | ", values);
    }

    // query·검색 provider/model/topK·candidate 유형·청크 규칙 버전으로 재사용 hash를 결정한다.
    private String queryHash(String queryText, String provider, String model, int topK, Set<UserDocumentType> types, String chunkingVersion) {
        String value = queryText + "|" + provider + "|" + model + "|" + topK + "|"
                + types.stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(",")) + "|" + chunkingVersion;
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    // 기존 result와 하위 chunk가 현재 계약·소유 범위·순위까지 일치할 때만 재사용한다.
    private boolean isReusable(RequirementRetrievalResult result, AnalysisInputSnapshot snapshot, Long userId,
                               RequirementInput requirement, String queryText, String queryHash, String provider, String model, int topK) {
        if (result == null || result.getSnapshot() == null || !snapshot.getSnapshotId().equals(result.getSnapshot().getSnapshotId())
                || !userId.equals(result.getUserId()) || !requirement.id().equals(result.getRequirementId())
                || result.getRequirementType() != requirement.type() || !requirement.text().equals(result.getRequirementText())
                || result.getCorpusType() != RetrievalCorpusType.CANDIDATE || !queryText.equals(result.getQueryText())
                || !queryHash.equals(result.getQueryHash()) || !provider.equals(result.getEmbeddingProvider())
                || !model.equals(result.getEmbeddingModel()) || result.getTopK() != topK
                || (result.getStatus() != RetrievalStatus.COMPLETED && result.getStatus() != RetrievalStatus.EMPTY)) return false;
        List<RequirementRetrievalChunk> chunks = retrievalChunkRepository
                .findByRetrievalResult_RetrievalResultIdOrderByRankAsc(result.getRetrievalResultId());
        if (result.getStatus() == RetrievalStatus.EMPTY) return chunks.isEmpty();
        if (chunks.isEmpty() || chunks.size() > topK) return false;
        Set<Long> materialChunkIds = new HashSet<>();
        for (int index = 0; index < chunks.size(); index++) {
            RequirementRetrievalChunk chunk = chunks.get(index);
            AnalysisMaterialChunk material = chunk.getMaterialChunk();
            if (chunk.getRank() != index + 1 || !Double.isFinite(chunk.getSimilarityScore()) || material == null
                    || material.getChunkId() == null || !materialChunkIds.add(material.getChunkId())
                    || (index > 0 && chunks.get(index - 1).getSimilarityScore() < chunk.getSimilarityScore())
                    || !userId.equals(material.getUserId()) || !snapshot.getSnapshotId().equals(material.getSnapshot().getSnapshotId())
                    || !CANDIDATE_DOCUMENT_TYPES.contains(material.getDocumentType())
                    || !com.example.jobpuzzle.analysis.service.AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION.equals(material.getChunkingVersion())) return false;
        }
        return true;
    }

    // FK와 unique 제약을 안전하게 해제한 뒤 flush해 같은 requirement key를 새로 저장할 수 있게 한다.
    private void deleteExisting(RequirementRetrievalResult existing) {
        retrievalChunkRepository.deleteByRetrievalResult_RetrievalResultId(existing.getRetrievalResultId());
        retrievalResultRepository.delete(existing);
        retrievalResultRepository.flush();
    }

    // adapter 결과의 rank·score·metadata와 실제 scoped material chunk를 대조해 외부 검색 오염을 차단한다.
    private List<ResolvedSearchResult> resolveAndValidateSearchResults(List<AnalysisMaterialChunkSearchResult> searched,
                                                                         Long userId, Long snapshotId, int topK) {
        if (searched == null) throw new IllegalArgumentException("vector search result must not be null");
        if (searched.size() > topK) throw new IllegalArgumentException("vector search returned more than topK");
        Set<Long> ids = new HashSet<>();
        for (int index = 0; index < searched.size(); index++) {
            AnalysisMaterialChunkSearchResult value = searched.get(index);
            if (value == null || value.chunkId() == null || value.snapshotSourceId() == null || value.documentId() == null
                    || value.documentType() == null || value.content() == null || value.rank() != index + 1 || !Double.isFinite(value.score())
                    || !ids.add(value.chunkId()) || (index > 0 && compareSearchResult(searched.get(index - 1), value) > 0)) {
                throw new IllegalArgumentException("vector search result order or value is invalid");
            }
        }
        if (searched.isEmpty()) return List.of();
        List<AnalysisMaterialChunk> materials = materialChunkRepository
                .findByChunkIdInAndUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersion(ids, userId, snapshotId,
                        CANDIDATE_DOCUMENT_TYPES, com.example.jobpuzzle.analysis.service.AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION);
        if (materials.size() != ids.size()) throw new IllegalArgumentException("vector search returned chunk outside scoped candidates");
        Map<Long, AnalysisMaterialChunk> materialById = materials.stream().collect(java.util.stream.Collectors.toMap(AnalysisMaterialChunk::getChunkId, value -> value));
        return searched.stream().map(value -> {
            AnalysisMaterialChunk material = materialById.get(value.chunkId());
            if (material == null || !sameMetadata(material, value)) {
                throw new IllegalArgumentException("vector search result metadata mismatch");
            }
            return new ResolvedSearchResult(material, value);
        }).toList();
    }

    // adapter의 score 및 동점 규칙이 저장 전에도 지켜졌는지 확인한다.
    private int compareSearchResult(AnalysisMaterialChunkSearchResult left, AnalysisMaterialChunkSearchResult right) {
        int score = Double.compare(right.score(), left.score());
        if (score != 0) return score;
        int source = Long.compare(left.snapshotSourceId(), right.snapshotSourceId());
        if (source != 0) return source;
        int index = Integer.compare(left.chunkIndex(), right.chunkIndex());
        return index != 0 ? index : Long.compare(left.chunkId(), right.chunkId());
    }

    private boolean sameMetadata(AnalysisMaterialChunk material, AnalysisMaterialChunkSearchResult value) {
        return material.getSnapshotSource() != null && material.getSnapshotSource().getSnapshotSourceId() != null
                && material.getDocumentId() != null && material.getContent() != null
                && material.getSnapshotSource().getSnapshotSourceId().equals(value.snapshotSourceId())
                && material.getDocumentId().equals(value.documentId()) && material.getDocumentType() == value.documentType()
                && material.getPageStart() == value.pageStart() && material.getPageEnd() == value.pageEnd()
                && material.getCharStart() == value.charStart() && material.getCharEnd() == value.charEnd()
                && material.getChunkIndex() == value.chunkIndex() && material.getContent().equals(value.content());
    }

    private String required(String value, String name) {
        if (blank(value)) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record RequirementInput(String id, RequirementType type, String text) {
    }
    private record ResolvedSearchResult(AnalysisMaterialChunk materialChunk, AnalysisMaterialChunkSearchResult searchResult) {
    }
}
