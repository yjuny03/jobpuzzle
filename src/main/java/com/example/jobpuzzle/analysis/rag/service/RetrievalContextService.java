package com.example.jobpuzzle.analysis.rag.service;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.analysis.entity.JobPostingAnalysis;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.analysis.rag.dto.RetrievedEvidenceContext;
import com.example.jobpuzzle.analysis.rag.dto.RetrievedEvidenceContextDto;
import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalChunk;
import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalResult;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalCorpusType;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalStatus;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalChunkRepository;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalResultRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.JobPostingAnalysisRepository;
import com.example.jobpuzzle.analysis.service.AnalysisMaterialChunkService;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 저장된 candidate retrieval만 읽어 JSON-05의 재현 가능한 근거 입력으로 조립한다. */
@Service
@RequiredArgsConstructor
public class RetrievalContextService {
    private static final Set<UserDocumentType> CANDIDATE_DOCUMENT_TYPES = EnumSet.of(
            UserDocumentType.RESUME, UserDocumentType.COVER_LETTER,
            UserDocumentType.PORTFOLIO, UserDocumentType.EXPERIENCE_NOTE);

    private final AnalysisInputSnapshotRepository snapshotRepository;
    private final JobPostingAnalysisRepository jobPostingRepository;
    private final RequirementRetrievalResultRepository retrievalResultRepository;
    private final RequirementRetrievalChunkRepository retrievalChunkRepository;

    // JSON-05 실행에서는 이미 고정된 retrieval 결과만 읽어 재현 가능한 근거 입력을 만든다.
    @Transactional(readOnly = true)
    public RetrievedEvidenceContext getCandidateEvidenceContext(Long userId, Long snapshotId) {
        if (userId == null || snapshotId == null) throw conflict("userId and snapshotId are required");
        AnalysisInputSnapshot snapshot = snapshotRepository.findById(snapshotId).orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));
        if (snapshot.getUser() == null || !userId.equals(snapshot.getUser().getUserId())) throw conflict("snapshot ownership mismatch");
        List<RequirementInput> requirements = requirements(validJobPosting(snapshotId));
        List<RequirementRetrievalResult> stored = retrievalResultRepository
                .findByUserIdAndSnapshot_SnapshotIdOrderByRetrievalResultIdAsc(userId, snapshotId);
        Map<String, RequirementRetrievalResult> resultByRequirement = resultsByRequirement(stored, snapshot, userId);
        if (resultByRequirement.size() != requirements.size()) throw conflict("JSON-01 and retrieval requirement count mismatch");

        List<RetrievedEvidenceContextDto.RequirementEvidence> evidence = new ArrayList<>();
        StringBuilder fingerprint = new StringBuilder();
        for (RequirementInput requirement : requirements) {
            RequirementRetrievalResult result = resultByRequirement.remove(requirement.id());
            if (result == null || result.getRequirementType() != requirement.type()
                    || !requirement.text().equals(result.getRequirementText())) throw conflict("retrieval requirement contract mismatch");
            List<RequirementRetrievalChunk> chunks = retrievalChunkRepository
                    .findByRetrievalResult_RetrievalResultIdOrderByRankAsc(result.getRetrievalResultId());
            List<RetrievedEvidenceContextDto.RetrievedChunk> mapped = validateAndMapChunks(result, chunks, snapshot, userId);
            evidence.add(new RetrievedEvidenceContextDto.RequirementEvidence(requirement.id(), requirement.type(), requirement.text(),
                    result.getStatus(), mapped));
            appendFingerprint(fingerprint, result, chunks);
        }
        if (!resultByRequirement.isEmpty()) throw conflict("retrieval contains unexpected requirement");
        return new RetrievedEvidenceContext(new RetrievedEvidenceContextDto(List.copyOf(evidence)), fingerprint.toString());
    }

    // JSON-01이 성공·valid 상태인 requirement 배열만 retrieval 완전성의 기준으로 사용한다.
    private JobPostingAnalysis validJobPosting(Long snapshotId) {
        JobPostingAnalysis posting = jobPostingRepository.findBySnapshot_SnapshotId(snapshotId)
                .orElseThrow(() -> conflict("JSON-01 result is missing"));
        AiCallLog log = posting.getAiCallLog();
        if (log == null || log.getStatus() != AiCallLogStatus.SUCCEEDED || !Boolean.TRUE.equals(log.getValid()))
            throw conflict("JSON-01 is not succeeded and valid");
        return posting;
    }

    // JSON-01의 REQUIRED 뒤 PREFERRED 순서를 JSON-05 retrieval 입력의 유일한 기준으로 고정한다.
    private List<RequirementInput> requirements(JobPostingAnalysis posting) {
        List<RequirementInput> values = new ArrayList<>();
        addRequirements(values, posting.getRequirements(), RequirementType.REQUIRED);
        addRequirements(values, posting.getPreferred(), RequirementType.PREFERRED);
        Set<String> ids = new HashSet<>();
        for (RequirementInput value : values) {
            if (blank(value.id()) || blank(value.text()) || !ids.add(value.id())) throw conflict("invalid JSON-01 requirement");
        }
        return values;
    }

    // null JSON 배열도 빈 requirement 목록으로 일관되게 취급한다.
    private void addRequirements(List<RequirementInput> target, List<JobPostingAnalysis.Requirement> source, RequirementType type) {
        if (source != null) source.forEach(value -> target.add(new RequirementInput(value.getRequirementId(), type, value.getText())));
    }

    // retrieval result의 snapshot·소유자·corpus·상태와 requirement ID 중복을 먼저 차단한다.
    private Map<String, RequirementRetrievalResult> resultsByRequirement(List<RequirementRetrievalResult> stored,
                                                                           AnalysisInputSnapshot snapshot, Long userId) {
        Map<String, RequirementRetrievalResult> values = new HashMap<>();
        for (RequirementRetrievalResult result : stored) {
            if (result == null || result.getRetrievalResultId() == null || result.getSnapshot() == null
                    || !snapshot.getSnapshotId().equals(result.getSnapshot().getSnapshotId()) || !userId.equals(result.getUserId())
                    || result.getCorpusType() != RetrievalCorpusType.CANDIDATE || blank(result.getRequirementId())
                    || blank(result.getRequirementText()) || blank(result.getQueryHash())
                    || (result.getStatus() != RetrievalStatus.COMPLETED && result.getStatus() != RetrievalStatus.EMPTY)
                    || values.put(result.getRequirementId(), result) != null) throw conflict("invalid candidate retrieval result");
        }
        return values;
    }

    // result의 모든 child chunk를 같은 snapshot 후보 범위에서 검증한 뒤 prompt DTO로 제한해 변환한다.
    private List<RetrievedEvidenceContextDto.RetrievedChunk> validateAndMapChunks(RequirementRetrievalResult result,
                                                                                     List<RequirementRetrievalChunk> chunks,
                                                                                     AnalysisInputSnapshot snapshot, Long userId) {
        if (chunks == null || (result.getStatus() == RetrievalStatus.EMPTY && !chunks.isEmpty())
                || (result.getStatus() == RetrievalStatus.COMPLETED && chunks.isEmpty())) throw conflict("retrieval status and chunks mismatch");
        Set<Long> materialIds = new HashSet<>();
        List<RetrievedEvidenceContextDto.RetrievedChunk> mapped = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            RequirementRetrievalChunk chunk = chunks.get(index);
            AnalysisMaterialChunk material = chunk == null ? null : chunk.getMaterialChunk();
            if (chunk == null || chunk.getRank() != index + 1 || !Double.isFinite(chunk.getSimilarityScore())
                    || (index > 0 && chunks.get(index - 1).getSimilarityScore() < chunk.getSimilarityScore())
                    || material == null || material.getChunkId() == null || !materialIds.add(material.getChunkId())
                    || material.getSnapshot() == null || !userId.equals(material.getUserId())
                    || !snapshot.getSnapshotId().equals(material.getSnapshot().getSnapshotId())
                    || material.getExtraction() == null || material.getExtraction().getExtractionId() == null
                    || !CANDIDATE_DOCUMENT_TYPES.contains(material.getDocumentType())
                    || !AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION.equals(material.getChunkingVersion())
                    || material.getDocumentId() == null || material.getContent() == null) throw conflict("invalid retrieval chunk");
            mapped.add(new RetrievedEvidenceContextDto.RetrievedChunk(material.getChunkId(), material.getExtraction().getExtractionId(),
                    material.getDocumentId(), material.getDocumentType(), material.getPageStart(), material.getPageEnd(),
                    material.getCharStart(), material.getCharEnd(), chunk.getRank(), chunk.getSimilarityScore(), material.getContent()));
        }
        return List.copyOf(mapped);
    }

    // retrieval ID·query hash·rank·score·material hash/version만 고정 순서로 fingerprint 재료에 넣는다.
    private void appendFingerprint(StringBuilder target, RequirementRetrievalResult result, List<RequirementRetrievalChunk> chunks) {
        target.append(result.getRetrievalResultId()).append('|').append(result.getRequirementId()).append('|')
                .append(result.getStatus()).append('|').append(result.getQueryHash()).append('\n');
        for (RequirementRetrievalChunk chunk : chunks) {
            AnalysisMaterialChunk material = chunk.getMaterialChunk();
            target.append(chunk.getRetrievalChunkId()).append('|').append(chunk.getRank()).append('|')
                    .append(Double.toString(chunk.getSimilarityScore())).append('|').append(material.getContentHash()).append('|')
                    .append(material.getChunkingVersion()).append('\n');
        }
    }

    // JSON-05에 사용할 고정 retrieval 계약이 깨지면 명확한 무결성 오류로 실행을 막는다.
    private CustomException conflict(String message) {
        return new CustomException(ErrorCode.JSON05_RESULT_INTEGRITY_CONFLICT, message);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record RequirementInput(String id, RequirementType type, String text) {
    }
}
