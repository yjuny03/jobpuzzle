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
import com.example.jobpuzzle.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuideContextServiceTest {

    private static final long USER_ID = 1L;
    private static final long SNAPSHOT_ID = 10L;

    @Mock private AnalysisInputSnapshotRepository snapshotRepository;
    @Mock private JobGuideDocumentRepository guideRepository;
    @Mock private JobGuideChunkRepository chunkRepository;
    @Mock private GuideContextResultRepository resultRepository;
    @Mock private GuideContextChunkRepository contextChunkRepository;
    @InjectMocks private GuideContextService guideContextService;

    private AnalysisInputSnapshot snapshot;
    private JobCategory snapshotCategory;

    @BeforeEach
    void setUp() {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(USER_ID);
        snapshotCategory = JobCategory.builder()
                .mainCategory("개발")
                .subCategory("백엔드")
                .careerLevel(JobCategoryCareerLevel.EXPERIENCED)
                .build();
        ReflectionTestUtils.setField(snapshotCategory, "jobCategoryId", 100L);
        snapshot = mock(AnalysisInputSnapshot.class);
        when(snapshot.getUser()).thenReturn(user);
        when(snapshot.getJobCategory()).thenReturn(snapshotCategory);
        when(snapshotRepository.findWithLockBySnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(snapshot));
        when(resultRepository.findByPurposeAndInputReferenceTypeAndInputReferenceId(any(), any(), anyString()))
                .thenReturn(Optional.empty());
        when(resultRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contextChunkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void usesExactSnapshotCategoryBeforeAnyFallback() {
        JobGuideDocument exactGuide = guide(11L, "정확 가이드");
        when(guideRepository.findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(
                GuideScopeType.CATEGORY, "개발", "백엔드", JobCategoryCareerLevel.EXPERIENCED, JobGuideDocumentStatus.ACTIVE))
                .thenReturn(List.of(exactGuide));
        when(chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(11L)).thenReturn(List.of());

        GuideContextResultDto result = guideContextService.getOrCreateCustomizedSynthesisGuideContext(USER_ID, SNAPSHOT_ID);

        assertThat(result.getMatchType()).isEqualTo(GuideMatchType.EXACT);
        assertThat(result.isFallbackApplied()).isFalse();
        assertThat(result.isInsufficient()).isFalse();
        verify(guideRepository, never()).findByScopeTypeAndScopeMainCategoryAndStatus(any(), anyString(), any());
    }

    @Test
    void fallsBackInDocumentedOrderAndKeepsScoreNull() {
        JobGuideDocument globalGuide = guide(12L, "공통 가이드");
        JobGuideChunk chunk = mock(JobGuideChunk.class);
        when(chunk.getChunkId()).thenReturn(90L);
        when(chunk.getTitle()).thenReturn("STAR");
        when(chunk.getContent()).thenReturn("내용");
        when(guideRepository.findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(any(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of());
        when(guideRepository.findByScopeTypeAndScopeMainCategoryAndStatus(GuideScopeType.PARENT_CATEGORY, "개발", JobGuideDocumentStatus.ACTIVE))
                .thenReturn(List.of());
        when(guideRepository.findByScopeTypeAndStatus(GuideScopeType.GLOBAL_COMMON, JobGuideDocumentStatus.ACTIVE))
                .thenReturn(List.of(globalGuide));
        when(chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(12L)).thenReturn(List.of(chunk));

        GuideContextResultDto result = guideContextService.getOrCreateCustomizedSynthesisGuideContext(USER_ID, SNAPSHOT_ID);

        assertThat(result.getMatchType()).isEqualTo(GuideMatchType.FALLBACK_COMMON);
        assertThat(result.isFallbackApplied()).isTrue();
        assertThat(result.getChunks()).singleElement().extracting(GuideContextResultDto.Chunk::getScore).isNull();
    }

    @Test
    void fallsBackToSameSubcategoryAnyBeforeParentCategory() {
        JobGuideDocument anyGuide = guide(13L, "동일 중분류 공통 가이드");
        when(guideRepository.findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(
                GuideScopeType.CATEGORY, "개발", "백엔드", JobCategoryCareerLevel.EXPERIENCED, JobGuideDocumentStatus.ACTIVE))
                .thenReturn(List.of());
        when(guideRepository.findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(
                GuideScopeType.CATEGORY, "개발", "백엔드", JobCategoryCareerLevel.ANY, JobGuideDocumentStatus.ACTIVE))
                .thenReturn(List.of(anyGuide));
        when(chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(13L)).thenReturn(List.of());

        GuideContextResultDto result = guideContextService.getOrCreateCustomizedSynthesisGuideContext(USER_ID, SNAPSHOT_ID);

        assertThat(result.getMatchType()).isEqualTo(GuideMatchType.FALLBACK_SAME_SUBCATEGORY);
        verify(guideRepository, never()).findByScopeTypeAndScopeMainCategoryAndStatus(any(), anyString(), any());
    }

    @Test
    void fallsBackToParentCategoryBeforeGlobalCommon() {
        JobGuideDocument parentGuide = guide(14L, "대분류 공통 가이드");
        when(guideRepository.findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(any(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of());
        when(guideRepository.findByScopeTypeAndScopeMainCategoryAndStatus(GuideScopeType.PARENT_CATEGORY, "개발", JobGuideDocumentStatus.ACTIVE))
                .thenReturn(List.of(parentGuide));
        when(chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(14L)).thenReturn(List.of());

        GuideContextResultDto result = guideContextService.getOrCreateCustomizedSynthesisGuideContext(USER_ID, SNAPSHOT_ID);

        assertThat(result.getMatchType()).isEqualTo(GuideMatchType.FALLBACK_PARENT_CATEGORY);
        verify(guideRepository, never()).findByScopeTypeAndStatus(GuideScopeType.GLOBAL_COMMON, JobGuideDocumentStatus.ACTIVE);
    }

    @Test
    void createsAndStoresReusableNoneResult() {
        when(guideRepository.findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(any(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of());
        when(guideRepository.findByScopeTypeAndScopeMainCategoryAndStatus(any(), anyString(), any())).thenReturn(List.of());
        when(guideRepository.findByScopeTypeAndStatus(any(), any())).thenReturn(List.of());

        GuideContextResultDto result = guideContextService.getOrCreateCustomizedSynthesisGuideContext(USER_ID, SNAPSHOT_ID);

        assertThat(result.getGuideId()).isNull();
        assertThat(result.getChunks()).isEmpty();
        assertThat(result.isInsufficient()).isTrue();
        assertThat(result.getInsufficientReason()).isNotBlank();
        verify(contextChunkRepository, never()).saveAll(any());
    }

    @Test
    void reusesExistingSnapshotResultWithoutSearchingAgain() {
        JobGuideDocument exactGuide = guide(15L, "기존 가이드");
        GuideContextResult existing = GuideContextResult.create(
                snapshot.getUser(), String.valueOf(SNAPSHOT_ID), snapshotCategory, exactGuide, GuideMatchType.EXACT);
        ReflectionTestUtils.setField(existing, "guideContextResultId", 77L);
        when(resultRepository.findByPurposeAndInputReferenceTypeAndInputReferenceId(
                GuideContextPurpose.CUSTOMIZED_SYNTHESIS, GuideContextInputReferenceType.ANALYSIS_SNAPSHOT, "10"))
                .thenReturn(Optional.of(existing));
        when(contextChunkRepository.findByGuideContextResult_GuideContextResultIdOrderByDisplayOrderAsc(77L))
                .thenReturn(List.of());

        GuideContextResultDto result = guideContextService.getOrCreateCustomizedSynthesisGuideContext(USER_ID, SNAPSHOT_ID);

        assertThat(result.getGuideId()).isEqualTo(15L);
        verifyNoInteractions(guideRepository, chunkRepository);
        verify(resultRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicatedActiveGuidesInsteadOfChoosingOne() {
        JobGuideDocument first = guide(1L, "첫 번째");
        JobGuideDocument second = guide(2L, "두 번째");
        when(guideRepository.findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(any(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> guideContextService.getOrCreateCustomizedSynthesisGuideContext(USER_ID, SNAPSHOT_ID))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.GUIDE_ACTIVE_DUPLICATED);
    }

    @Test
    void rejectsSnapshotOwnedByAnotherUser() {
        assertThatThrownBy(() -> guideContextService.getOrCreateCustomizedSynthesisGuideContext(999L, SNAPSHOT_ID))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.SNAPSHOT_NOT_FOUND);
        verifyNoInteractions(guideRepository);
    }

    private JobGuideDocument guide(Long guideId, String title) {
        JobGuideDocument guide = mock(JobGuideDocument.class);
        when(guide.getGuideId()).thenReturn(guideId);
        when(guide.getTitle()).thenReturn(title);
        when(guide.getVersion()).thenReturn("v1.0");
        when(guide.getApplicableScope()).thenReturn("가이드 적용 범위");
        when(guide.getEvaluationFocus()).thenReturn(List.of("근거"));
        when(guide.getEvidenceRules()).thenReturn(List.of("구체성"));
        when(guide.getQuestionDirection()).thenReturn(List.of("질문"));
        when(guide.getAvoidQuestions()).thenReturn(List.of("회피"));
        return guide;
    }
}
