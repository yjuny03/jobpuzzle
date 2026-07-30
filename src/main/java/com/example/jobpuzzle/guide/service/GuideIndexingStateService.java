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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class GuideIndexingStateService {

    private final JobGuideDocumentRepository guideRepository;
    private final JobGuideChunkRepository chunkRepository;

    /** 최신 DRAFT의 현재 청크를 고정하고 중복 색인을 막는 짧은 트랜잭션을 시작한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GuideIndexingLease begin(Long guideId) {
        JobGuideDocument guide = findLocked(guideId);
        requireLatestDraft(guide);
        if (guide.isIndexingInProgress()) {
            throw new CustomException(ErrorCode.GUIDE_INDEXING_IN_PROGRESS);
        }
        List<JobGuideChunk> chunks =
                chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(guideId);
        if (chunks.isEmpty()) {
            throw new CustomException(ErrorCode.GUIDE_CHUNKS_REQUIRED);
        }
        guide.startIndexing();
        log.info("가이드 색인 상태 변경 guideId={} status=INDEXING chunkCount={}",
                guideId, chunks.size());
        return new GuideIndexingLease(
                guideId,
                chunks.stream().map(chunk -> new GuideVectorDocument(
                        chunk.getChunkId(),
                        guideId,
                        chunk.getContent()))
                        .toList());
    }

    /** Qdrant가 반환한 모든 청크 참조를 검증한 뒤 DB 색인 상태와 embedding_ref를 함께 확정한다. */
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
        log.info("가이드 색인 결과 저장 guideId={} status=INDEXED provider={} model={} dimension={}",
                guideId, provider, model, dimension);
        return GuideListResponse.from(guide, chunks.size());
    }

    /** 외부 임베딩·벡터 저장 실패를 별도 트랜잭션으로 기록해 재시도 가능한 상태로 남긴다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long guideId, String safeMessage) {
        JobGuideDocument guide = findLocked(guideId);
        if (guide.isDraft() && guide.isIndexingInProgress()) {
            guide.failIndexing(limit(safeMessage, 500));
            log.warn("가이드 색인 실패 상태 저장 guideId={} status=FAILED", guideId);
        }
    }

    /** 색인 상태가 동시에 변경되지 않도록 가이드 행을 쓰기 잠금으로 조회한다. */
    private JobGuideDocument findLocked(Long guideId) {
        return guideRepository.findWithLockByGuideId(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));
    }

    /** 이전 버전의 청크가 다시 색인되어 현재 분석에 섞이지 않도록 최신 초안만 허용한다. */
    private void requireLatestDraft(JobGuideDocument guide) {
        if (!guide.isDraft()) {
            throw new CustomException(ErrorCode.GUIDE_NOT_DRAFT);
        }
        if (guideRepository.existsByPreviousGuide_GuideId(guide.getGuideId())) {
            throw new CustomException(ErrorCode.GUIDE_NOT_LATEST_VERSION);
        }
    }

    /** DB 오류 필드 길이를 넘지 않도록 안전한 실패 문구를 제한한다. */
    private String limit(String message, int maxLength) {
        String safe = message == null || message.isBlank()
                ? ErrorCode.GUIDE_INDEXING_FAILED.getMessage() : message;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
