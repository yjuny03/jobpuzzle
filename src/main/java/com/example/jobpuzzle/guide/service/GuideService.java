package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.*;
import com.example.jobpuzzle.guide.entity.*;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 관리자가 분석 가이드를 등록하고 버전 계보를 관리하는 서비스.
 *
 * <p>신규 분석은 적용 범위별 ACTIVE 한 건만 사용한다. 새 버전을 활성화할 때
 * 이전 ACTIVE를 INACTIVE로 바꾸되, 이미 생성된 분석 결과의 가이드 스냅샷은
 * 수정하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GuideService {

    private final JobGuideDocumentRepository guideRepository;
    private final JobGuideChunkRepository chunkRepository;
    private final JobCategoryRepository jobCategoryRepository;

    /** 새 가이드 계보의 첫 DRAFT 버전을 등록한다. */
    public GuideListResponse registerGuideDocument(User admin, GuideRegisterRequest request) {
        requireAdmin(admin);
        if (guideRepository.existsByGuideCode(request.getGuideCode())) {
            throw new CustomException(ErrorCode.GUIDE_CODE_DUPLICATED);
        }
        if (guideRepository.existsByGuideCodeAndVersion(request.getGuideCode(), request.getVersion())) {
            throw new CustomException(ErrorCode.GUIDE_VERSION_DUPLICATED);
        }

        JobGuideDocument guide = buildGuide(
                admin, null, request.getGuideCode(), request.getScopeType(),
                resolveCategory(request.getScopeType(), request.getJobCategoryId()),
                request.getScopeMainCategory(), request.getTitle(), request.getSourceType(),
                request.getFilePath(), request.getVersion(), request.getApplicableScope(),
                request.getEvaluationFocus(), request.getEvidenceRules(),
                request.getQuestionDirection(), request.getAvoidQuestions()
        );
        return toResponse(guideRepository.save(guide));
    }

    /**
     * 계보의 최신(후속 버전이 없는) 가이드를 기준으로 다음 DRAFT 버전을 만든다.
     * 적용 범위와 가이드 코드는 이전 버전에서 강제로 상속한다.
     */
    public GuideListResponse createNextVersion(
            User admin, Long previousGuideId, GuideVersionCreateRequest request
    ) {
        requireAdmin(admin);
        JobGuideDocument previous = findLockedGuide(previousGuideId);
        requireLatest(previous);
        if (guideRepository.existsByGuideCodeAndVersion(previous.getGuideCode(), request.getVersion())) {
            throw new CustomException(ErrorCode.GUIDE_VERSION_DUPLICATED);
        }

        JobGuideDocument next = buildGuide(
                admin, previous, previous.getGuideCode(), previous.getScopeType(),
                previous.getJobCategory(), previous.getScopeMainCategory(),
                request.getTitle(), request.getSourceType(), request.getFilePath(),
                request.getVersion(), request.getApplicableScope(), request.getEvaluationFocus(),
                request.getEvidenceRules(), request.getQuestionDirection(), request.getAvoidQuestions()
        );
        return toResponse(guideRepository.save(next));
    }

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
        guide.markManuallyReadyForReview();
        return GuideListResponse.from(guide, entities.size());
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
        if (!target.isReadyForReview()) {
            throw new CustomException(ErrorCode.GUIDE_NOT_REVIEW_READY);
        }
        if (!target.isIndexed()) {
            throw new CustomException(ErrorCode.GUIDE_INDEX_NOT_READY);
        }

        findActiveGuidesInSameScope(target).stream()
                .filter(active -> !active.getGuideId().equals(target.getGuideId()))
                .forEach(JobGuideDocument::deactivate);
        target.activate();
        return toResponse(target);
    }

    /** 관리 화면에서 특정 버전과 청크 수를 조회한다. */
    @Transactional(readOnly = true)
    public GuideListResponse getGuide(Long guideId) {
        JobGuideDocument guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));
        return toResponse(guide);
    }

    private JobGuideDocument buildGuide(
            User admin, JobGuideDocument previous, String guideCode, GuideScopeType scopeType,
            JobCategory category, String scopeMainCategory, String title,
            JobGuideDocumentSourceType sourceType, String filePath, String version,
            String applicableScope, List<String> evaluationFocus, List<String> evidenceRules,
            List<String> questionDirection, List<String> avoidQuestions
    ) {
        return JobGuideDocument.builder()
                .guideCode(guideCode.trim())
                .previousGuide(previous)
                .scopeType(scopeType)
                .jobCategory(category)
                .scopeMainCategory(normalize(scopeMainCategory))
                .title(title.trim())
                .sourceType(sourceType)
                .filePath(normalize(filePath))
                .version(version.trim())
                .createdBy(admin)
                .applicableScope(applicableScope.trim())
                .evaluationFocus(copyOrEmpty(evaluationFocus))
                .evidenceRules(copyOrEmpty(evidenceRules))
                .questionDirection(copyOrEmpty(questionDirection))
                .avoidQuestions(copyOrEmpty(avoidQuestions))
                .build();
    }

    private JobCategory resolveCategory(GuideScopeType scopeType, Long categoryId) {
        if (scopeType != GuideScopeType.CATEGORY) {
            return null;
        }
        if (categoryId == null) {
            throw new CustomException(ErrorCode.GUIDE_SCOPE_INVALID);
        }
        return jobCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND));
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

    private List<String> copyOrEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
