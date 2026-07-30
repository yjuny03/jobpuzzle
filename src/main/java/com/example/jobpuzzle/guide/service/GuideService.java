package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.*;
import com.example.jobpuzzle.guide.entity.*;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 관리자가 저장된 가이드 청크를 검수하고 색인 완료 버전을 활성화하는 서비스.
 *
 * <p>신규 분석은 적용 범위별 ACTIVE 한 건만 사용한다. 새 버전을 활성화할 때
 * 이전 ACTIVE를 INACTIVE로 바꾸되, 이미 생성된 분석 결과의 가이드 스냅샷은
 * 수정하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GuideService {

    private final JobGuideDocumentRepository guideRepository;
    private final JobGuideChunkRepository chunkRepository;

    /**
     * DRAFT의 검수된 청크를 전체 교체한다.
     * 부분 갱신 대신 전체 교체를 사용해 중복·누락된 인덱스를 남기지 않는다.
     */
    public GuideListResponse replaceDraftChunks(Long guideId, GuideChunkReplaceRequest request) {
        JobGuideDocument guide = findLockedGuide(guideId);
        requireDraft(guide);
        if (guide.isIndexingInProgress()) {
            throw new CustomException(ErrorCode.GUIDE_INDEXING_IN_PROGRESS);
        }

        List<GuideChunkRequest> chunks = new ArrayList<>(request.getChunks());
        chunks.sort(Comparator.comparingInt(GuideChunkRequest::getChunkIndex));
        for (int index = 0; index < chunks.size(); index++) {
            if (chunks.get(index).getChunkIndex() != index) {
                throw new CustomException(ErrorCode.GUIDE_CHUNK_ORDER_INVALID);
            }
        }

        chunkRepository.deleteByGuide_GuideId(guideId);
        // 같은 guide_id·chunk_index를 다시 넣기 전에 DELETE를 확정해 유니크 키 충돌을 막는다.
        chunkRepository.flush();
        List<JobGuideChunk> entities = chunks.stream()
                .map(chunk -> JobGuideChunk.builder()
                        .guide(guide)
                        .chunkIndex(chunk.getChunkIndex())
                        .title(chunk.getTitle())
                        .content(chunk.getContent())
                        .contentSummary(chunk.getContentSummary())
                        .embeddingRef(null)
                        .build())
                .toList();
        chunkRepository.saveAll(entities);
        guide.resetIndexingForChunkChange();
        log.info("관리자 검수 청크 저장 guideId={} chunkCount={} indexingStatus=NOT_INDEXED",
                guideId, entities.size());
        return GuideListResponse.from(guide, entities.size());
    }

    /** 청크 순서를 보존해 관리자 검수 화면에 반환한다. */
    @Transactional(readOnly = true)
    public List<GuideChunkResponse> getChunks(Long guideId) {
        if (!guideRepository.existsById(guideId)) {
            throw new CustomException(ErrorCode.GUIDE_NOT_FOUND);
        }
        List<GuideChunkResponse> chunks =
                chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(guideId).stream()
                        .map(GuideChunkResponse::from)
                        .toList();
        log.debug("가이드 검수 청크 조회 guideId={} chunkCount={}", guideId, chunks.size());
        return chunks;
    }

    /**
     * 최신 DRAFT를 실제 분석에서 사용하는 버전으로 전환한다.
     * 같은 적용 범위의 기존 ACTIVE는 같은 트랜잭션에서 먼저 비활성화한다.
     */
    public GuideListResponse activateLatestVersion(User admin, Long guideId) {
        requireAdmin(admin);
        JobGuideDocument target = findLockedGuide(guideId);
        requireDraft(target);
        requireLatest(target);
        if (chunkRepository.countByGuide_GuideId(guideId) == 0) {
            throw new CustomException(ErrorCode.GUIDE_CHUNKS_REQUIRED);
        }
        if (!target.isIndexed()) {
            throw new CustomException(ErrorCode.GUIDE_INDEX_NOT_READY);
        }

        List<JobGuideDocument> previousActiveGuides = findActiveGuidesInSameScope(target).stream()
                .filter(active -> !active.getGuideId().equals(target.getGuideId()))
                .toList();
        previousActiveGuides.forEach(JobGuideDocument::deactivate);
        target.activate();
        // 기존 ACTIVE 비활성화와 최신 DRAFT 활성화를 한 트랜잭션에 묶어 무가이드 구간을 만들지 않는다.
        log.info("최신 가이드 활성화 guideId={} guideCode={} version={} replacedActiveCount={}",
                guideId, target.getGuideCode(), target.getVersion(), previousActiveGuides.size());
        return toResponse(target);
    }


    private List<JobGuideDocument> findActiveGuidesInSameScope(JobGuideDocument guide) {
        return switch (guide.getScopeType()) {
            case CATEGORY -> guideRepository.findByScopeTypeAndJobCategory_JobCategoryIdAndStatus(
                    GuideScopeType.CATEGORY, guide.getJobCategory().getJobCategoryId(),
                    JobGuideDocumentStatus.ACTIVE);
            case PARENT_CATEGORY ->
                    guideRepository.findByScopeTypeAndScopeMainCategoryAndStatusOrderByGuideIdAsc(
                            GuideScopeType.PARENT_CATEGORY, guide.getScopeMainCategory(),
                            JobGuideDocumentStatus.ACTIVE);
            case GLOBAL_COMMON -> guideRepository.findByScopeTypeAndStatusOrderByGuideIdAsc(
                    GuideScopeType.GLOBAL_COMMON, JobGuideDocumentStatus.ACTIVE);
        };
    }

    private JobGuideDocument findLockedGuide(Long guideId) {
        return guideRepository.findWithLockByGuideId(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));
    }

    private void requireDraft(JobGuideDocument guide) {
        if (!guide.isDraft()) {
            throw new CustomException(ErrorCode.GUIDE_NOT_DRAFT);
        }
    }

    private void requireLatest(JobGuideDocument guide) {
        if (guideRepository.existsByPreviousGuide_GuideId(guide.getGuideId())) {
            throw new CustomException(ErrorCode.GUIDE_NOT_LATEST_VERSION);
        }
    }

    private void requireAdmin(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("관리자만 가이드를 관리할 수 있습니다.");
        }
    }

    private GuideListResponse toResponse(JobGuideDocument guide) {
        return GuideListResponse.from(
                guide, Math.toIntExact(chunkRepository.countByGuide_GuideId(guide.getGuideId())));
    }

}
