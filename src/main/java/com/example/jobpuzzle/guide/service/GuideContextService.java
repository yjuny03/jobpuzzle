package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.GuideContextResultDto;
import com.example.jobpuzzle.guide.entity.*;
import com.example.jobpuzzle.guide.repository.GuideContextChunkRepository;
import com.example.jobpuzzle.guide.repository.GuideContextResultRepository;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// JSON-04 가이드 검색 결과를 스냅샷 단위로 고정·재사용하는 운영 서비스다.
@Service
@RequiredArgsConstructor
public class GuideContextService {

    private final AnalysisInputSnapshotRepository analysisInputSnapshotRepository;
    private final JobGuideDocumentRepository jobGuideDocumentRepository;
    private final JobGuideChunkRepository jobGuideChunkRepository;
    private final GuideContextResultRepository guideContextResultRepository;
    private final GuideContextChunkRepository guideContextChunkRepository;

    // 사용자 소유 스냅샷에 대해 JSON-04 결과를 조회하거나 한 번만 생성한다.
    @Transactional
    public GuideContextResultDto getOrCreateCustomizedSynthesisGuideContext(Long userId, Long snapshotId) {
        // 같은 스냅샷의 동시 생성은 짧은 스냅샷 잠금으로 직렬화한다.
        AnalysisInputSnapshot snapshot = analysisInputSnapshotRepository.findWithLockBySnapshotId(snapshotId)
                .filter(value -> value.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));

        String inputReferenceId = String.valueOf(snapshotId);
        // 기존 JSON-04 결과는 이후 가이드 변경과 무관하게 그대로 재사용한다.
        GuideContextResult existing = findExisting(inputReferenceId);
        if (existing != null) {
            // 과거 정책이 남긴 NONE context는 정상 결과로 재사용하지 않고 분석을 중단한다.
            if (existing.getMatchType() == GuideMatchType.NONE) {
                throw new CustomException(ErrorCode.GUIDE_ACTIVE_NOT_FOUND);
            }
            return toDto(existing);
        }

        // 스냅샷에 고정된 직무·경력만으로 fallback 검색 기준을 만든다.
        JobCategory jobCategory = snapshot.getJobCategory();
        GuideSelection selection = findGuide(jobCategory);
        // 활성 가이드가 없으면 JSON-04 결과를 저장하지 않고 상위 파이프라인 실패 처리로 넘긴다.
        if (selection == null) {
            throw new CustomException(ErrorCode.GUIDE_ACTIVE_NOT_FOUND);
        }
        // flush 실패 뒤에는 같은 영속성 컨텍스트를 재조회하지 않고 트랜잭션을 종료시킨다.
        GuideContextResult result = guideContextResultRepository.saveAndFlush(
                GuideContextResult.create(snapshot.getUser(), inputReferenceId, jobCategory,
                        selection.guide(), selection.matchType()));
        List<GuideContextChunk> contextChunks = saveChunks(result, selection.guide());
        return GuideContextResultDto.from(result, contextChunks);
    }

    private GuideContextResult findExisting(String inputReferenceId) {
        return guideContextResultRepository.findByPurposeAndInputReferenceTypeAndInputReferenceId(
                GuideContextPurpose.CUSTOMIZED_SYNTHESIS,
                GuideContextInputReferenceType.ANALYSIS_SNAPSHOT,
                inputReferenceId).orElse(null);
    }

    private GuideContextResultDto toDto(GuideContextResult result) {
        return GuideContextResultDto.from(result,
                guideContextChunkRepository.findByGuideContextResult_GuideContextResultIdOrderByDisplayOrderAsc(
                        result.getGuideContextResultId()));
    }

    private GuideSelection findGuide(JobCategory jobCategory) {
        // 대분류·중분류·경력이 모두 일치하는 CATEGORY 가이드를 먼저 찾는다.
        JobGuideDocument exact = oneCandidate(jobGuideDocumentRepository
                .findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(
                        GuideScopeType.CATEGORY, jobCategory.getMainCategory(), jobCategory.getSubCategory(),
                        jobCategory.getCareerLevel(), JobGuideDocumentStatus.ACTIVE));
        if (exact != null) {
            return new GuideSelection(exact, GuideMatchType.EXACT);
        }

        // 동일 중분류의 ANY 경력 CATEGORY 가이드를 두 번째 fallback으로 찾는다.
        JobGuideDocument subcategoryFallback = oneCandidate(jobGuideDocumentRepository
                .findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(
                        GuideScopeType.CATEGORY, jobCategory.getMainCategory(), jobCategory.getSubCategory(),
                        JobCategoryCareerLevel.ANY, JobGuideDocumentStatus.ACTIVE));
        if (subcategoryFallback != null) {
            return new GuideSelection(subcategoryFallback, GuideMatchType.FALLBACK_SAME_SUBCATEGORY);
        }

        // 대분류 전체에 적용되는 PARENT_CATEGORY 가이드를 세 번째로 찾는다.
        JobGuideDocument parentFallback = oneCandidate(jobGuideDocumentRepository
                .findByScopeTypeAndScopeMainCategoryAndStatus(GuideScopeType.PARENT_CATEGORY,
                        jobCategory.getMainCategory(), JobGuideDocumentStatus.ACTIVE));
        if (parentFallback != null) {
            return new GuideSelection(parentFallback, GuideMatchType.FALLBACK_PARENT_CATEGORY);
        }

        // 모든 직무에 적용되는 GLOBAL_COMMON 가이드를 마지막 fallback으로 찾는다.
        JobGuideDocument commonFallback = oneCandidate(jobGuideDocumentRepository
                .findByScopeTypeAndStatus(GuideScopeType.GLOBAL_COMMON, JobGuideDocumentStatus.ACTIVE));
        if (commonFallback != null) {
            return new GuideSelection(commonFallback, GuideMatchType.FALLBACK_COMMON);
        }
        // 가이드가 없으면 NONE fallback을 만들지 않고 호출자에게 명시적 오류를 전달한다.
        return null;
    }

    // 같은 검색 범위의 ACTIVE 가이드가 여러 건이면 임의 선택하지 않고 데이터 정합 오류로 처리한다.
    private JobGuideDocument oneCandidate(List<JobGuideDocument> candidates) {
        if (candidates.size() > 1) {
            throw new CustomException(ErrorCode.GUIDE_ACTIVE_DUPLICATED);
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    // 선택 가이드의 모든 청크를 chunkIndex 순서대로 0부터 고정 저장한다.
    private List<GuideContextChunk> saveChunks(GuideContextResult result, JobGuideDocument guide) {
        if (guide == null) {
            return List.of();
        }
        List<JobGuideChunk> chunks = jobGuideChunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(guide.getGuideId());
        List<GuideContextChunk> contextChunks = java.util.stream.IntStream.range(0, chunks.size())
                .mapToObj(index -> GuideContextChunk.create(result, chunks.get(index), index))
                .toList();
        return guideContextChunkRepository.saveAll(contextChunks);
    }

    private record GuideSelection(JobGuideDocument guide, GuideMatchType matchType) {
    }
}
