package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.GuideListResponse;
import com.example.jobpuzzle.guide.entity.JobGuideChunk;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.guide.vector.GuideVectorDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 가이드 인덱싱의 선점·성공·실패 상태를 각각 짧은 트랜잭션으로 저장한다. */
@Service
@RequiredArgsConstructor
public class GuideIndexingStateService {

    private final JobGuideDocumentRepository guideRepository;
    private final JobGuideChunkRepository chunkRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GuideIndexingLease begin(Long guideId) {
        JobGuideDocument guide = findLocked(guideId);
        requireLatestDraft(guide);
        if (!guide.isReadyForReview()) {
            throw new CustomException(ErrorCode.GUIDE_NOT_REVIEW_READY);
        }
        if (guide.isIndexingInProgress()) {
            throw new CustomException(ErrorCode.GUIDE_INDEXING_IN_PROGRESS);
        }
        List<JobGuideChunk> chunks =
                chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(guideId);
        if (chunks.isEmpty()) {
            throw new CustomException(ErrorCode.GUIDE_CHUNKS_REQUIRED);
        }
        guide.startIndexing();
        return new GuideIndexingLease(
                guideId,
                chunks.stream().map(chunk -> new GuideVectorDocument(
                        chunk.getChunkId(),
                        guideId,
                        guide.getGuideCode(),
                        guide.getVersion(),
                        chunk.getChunkIndex(),
                        chunk.getTitle(),
                        chunk.getContent()))
                        .toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GuideListResponse complete(
            Long guideId,
            Map<Long, String> references,
            String provider,
            String model,
            int dimension
    ) {
        JobGuideDocument guide = findLocked(guideId);
        requireLatestDraft(guide);
        if (!guide.isReadyForReview()) {
            throw new CustomException(ErrorCode.GUIDE_NOT_REVIEW_READY);
        }
        if (!guide.isIndexingInProgress()) {
            throw new CustomException(ErrorCode.GUIDE_VECTOR_RESULT_INVALID);
        }
        List<JobGuideChunk> chunks =
                chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(guideId);
        Set<Long> expected = chunks.stream().map(JobGuideChunk::getChunkId).collect(Collectors.toSet());
        if (references == null || !references.keySet().equals(expected)
                || references.values().stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new CustomException(ErrorCode.GUIDE_VECTOR_RESULT_INVALID);
        }
        chunks.forEach(chunk -> chunk.markEmbedded(references.get(chunk.getChunkId())));
        guide.completeIndexing(provider, model, dimension);
        return GuideListResponse.from(guide, chunks.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long guideId, String safeMessage) {
        JobGuideDocument guide = findLocked(guideId);
        if (guide.isDraft() && guide.isIndexingInProgress()) {
            guide.failIndexing(limit(safeMessage, 500));
        }
    }

    private JobGuideDocument findLocked(Long guideId) {
        return guideRepository.findWithLockByGuideId(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));
    }

    private void requireLatestDraft(JobGuideDocument guide) {
        if (!guide.isDraft()) {
            throw new CustomException(ErrorCode.GUIDE_NOT_DRAFT);
        }
        if (guideRepository.existsByPreviousGuide_GuideId(guide.getGuideId())) {
            throw new CustomException(ErrorCode.GUIDE_NOT_LATEST_VERSION);
        }
    }

    private String limit(String message, int maxLength) {
        String safe = message == null || message.isBlank()
                ? ErrorCode.GUIDE_INDEXING_FAILED.getMessage() : message;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
