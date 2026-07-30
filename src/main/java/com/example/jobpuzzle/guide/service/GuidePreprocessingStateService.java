package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.GuideListResponse;
import com.example.jobpuzzle.guide.dto.GuidePreprocessingResult;
import com.example.jobpuzzle.guide.entity.JobGuideChunk;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 가이드 전처리 상태 변경만 짧은 독립 트랜잭션으로 처리한다.
 * 외부 OpenAI 호출 중에는 DB 비관적 잠금을 유지하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class GuidePreprocessingStateService {

    private final JobGuideDocumentRepository guideRepository;
    private final JobGuideChunkRepository chunkRepository;

    /** 호출 중복을 막기 위해 최신 DRAFT를 PROCESSING으로 선점한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void begin(Long guideId) {
        JobGuideDocument guide = findLocked(guideId);
        requireDraftAndLatest(guide);
        if (guide.isIndexingInProgress()) {
            throw new CustomException(ErrorCode.GUIDE_INDEXING_IN_PROGRESS);
        }
        if (guide.isPreprocessingInProgress()) {
            throw new CustomException(ErrorCode.GUIDE_PREPROCESSING_IN_PROGRESS);
        }
        guide.startPreprocessing();
    }

    /** 구조화 결과와 청크를 원자적으로 교체하고 관리자 검수 준비 상태로 변경한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GuideListResponse complete(
            Long guideId, String model, GuidePreprocessingResult result
    ) {
        JobGuideDocument guide = findLocked(guideId);
        requireDraftAndLatest(guide);
        if (!guide.isPreprocessingInProgress()) {
            throw new CustomException(ErrorCode.GUIDE_PREPROCESSING_RESPONSE_INVALID);
        }
        validateResult(result);

        chunkRepository.deleteByGuide_GuideId(guideId);
        List<JobGuideChunk> chunks = java.util.stream.IntStream.range(0, result.chunks().size())
                .mapToObj(index -> {
                    GuidePreprocessingResult.Chunk chunk = result.chunks().get(index);
                    return JobGuideChunk.builder()
                            .guide(guide)
                            .chunkIndex(index)
                            .title(chunk.title().trim())
                            .content(chunk.content().trim())
                            .contentSummary(chunk.contentSummary().trim())
                            .embeddingRef(null)
                            .build();
                })
                .toList();
        chunkRepository.saveAll(chunks);
        guide.completePreprocessing(
                model,
                result.applicableScope().trim(),
                clean(result.evaluationFocus()),
                clean(result.evidenceRules()),
                clean(result.questionDirection()),
                clean(result.avoidQuestions())
        );
        return GuideListResponse.from(guide, chunks.size());
    }

    /** Provider 실패 후에도 DRAFT를 보존하고 안전한 오류 요약만 기록한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long guideId, String safeMessage) {
        JobGuideDocument guide = findLocked(guideId);
        if (guide.isDraft() && guide.isPreprocessingInProgress()) {
            guide.failPreprocessing(limit(safeMessage, 500));
        }
    }

    private JobGuideDocument findLocked(Long guideId) {
        return guideRepository.findWithLockByGuideId(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));
    }

    private void requireDraftAndLatest(JobGuideDocument guide) {
        if (!guide.isDraft()) {
            throw new CustomException(ErrorCode.GUIDE_NOT_DRAFT);
        }
        if (guideRepository.existsByPreviousGuide_GuideId(guide.getGuideId())) {
            throw new CustomException(ErrorCode.GUIDE_NOT_LATEST_VERSION);
        }
    }

    private void validateResult(GuidePreprocessingResult result) {
        if (result == null
                || blank(result.applicableScope())
                || result.evaluationFocus() == null
                || result.evidenceRules() == null
                || result.questionDirection() == null
                || result.avoidQuestions() == null
                || result.chunks() == null
                || result.chunks().isEmpty()
                || result.chunks().stream().anyMatch(chunk ->
                        chunk == null || blank(chunk.title())
                                || blank(chunk.content()) || blank(chunk.contentSummary()))) {
            throw new CustomException(ErrorCode.GUIDE_PREPROCESSING_RESPONSE_INVALID);
        }
    }

    private List<String> clean(List<String> values) {
        if (values.stream().anyMatch(this::blank)) {
            throw new CustomException(ErrorCode.GUIDE_PREPROCESSING_RESPONSE_INVALID);
        }
        return values.stream().map(String::trim).distinct().toList();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String limit(String message, int maxLength) {
        String safe = blank(message) ? ErrorCode.GUIDE_PREPROCESSING_FAILED.getMessage() : message;
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }
}
