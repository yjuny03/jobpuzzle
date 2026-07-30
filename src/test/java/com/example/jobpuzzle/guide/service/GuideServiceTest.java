package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.GuideChunkReplaceRequest;
import com.example.jobpuzzle.guide.dto.GuideChunkRequest;
import com.example.jobpuzzle.guide.entity.*;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuideServiceTest {

    @Mock private JobGuideDocumentRepository guideRepository;
    @Mock private JobGuideChunkRepository chunkRepository;
    @InjectMocks private GuideService guideService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(chunkRepository.countByGuide_GuideId(any())).thenReturn(0L);
    }

    @Test
    void activatingLatestDraftDeactivatesPreviousActiveInSameScope() {
        JobGuideDocument previousActive =
                guide(10L, "BACKEND", "v1.0", JobGuideDocumentStatus.ACTIVE);
        JobGuideDocument latestDraft =
                guide(11L, "BACKEND", "v1.1", JobGuideDocumentStatus.DRAFT);
        latestDraft.completeIndexing("fake", "test-model", 64);
        when(guideRepository.findWithLockByGuideId(11L)).thenReturn(Optional.of(latestDraft));
        when(guideRepository.existsByPreviousGuide_GuideId(11L)).thenReturn(false);
        when(chunkRepository.countByGuide_GuideId(11L)).thenReturn(2L);
        when(guideRepository.findByScopeTypeAndStatusOrderByGuideIdAsc(
                GuideScopeType.GLOBAL_COMMON, JobGuideDocumentStatus.ACTIVE))
                .thenReturn(List.of(previousActive));

        guideService.activateLatestVersion(admin, 11L);

        assertThat(previousActive.getStatus()).isEqualTo(JobGuideDocumentStatus.INACTIVE);
        assertThat(latestDraft.getStatus()).isEqualTo(JobGuideDocumentStatus.ACTIVE);
    }

    @Test
    void cannotActivateDraftBeforeVectorIndexCompletes() {
        JobGuideDocument draft =
                guide(11L, "BACKEND", "v1.1", JobGuideDocumentStatus.DRAFT);
        when(guideRepository.findWithLockByGuideId(11L)).thenReturn(Optional.of(draft));
        when(guideRepository.existsByPreviousGuide_GuideId(11L)).thenReturn(false);
        when(chunkRepository.countByGuide_GuideId(11L)).thenReturn(2L);

        assertThatThrownBy(() -> guideService.activateLatestVersion(admin, 11L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUIDE_INDEX_NOT_READY);
        assertThat(draft.getStatus()).isEqualTo(JobGuideDocumentStatus.DRAFT);
    }

    @Test
    void rejectsChunkIndexesWithGapsBeforeReplacingStoredChunks() {
        JobGuideDocument draft =
                guide(11L, "BACKEND", "v1.1", JobGuideDocumentStatus.DRAFT);
        GuideChunkReplaceRequest request = mock(GuideChunkReplaceRequest.class);
        GuideChunkRequest first = chunk(0);
        GuideChunkRequest third = chunk(2);
        when(request.getChunks()).thenReturn(List.of(first, third));
        when(guideRepository.findWithLockByGuideId(11L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> guideService.replaceDraftChunks(11L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUIDE_CHUNK_ORDER_INVALID);
        verify(chunkRepository, never()).deleteByGuide_GuideId(any());
    }

    @Test
    void flushesDeletedChunksBeforeSavingReplacementWithSameIndexes() {
        JobGuideDocument draft =
                guide(11L, "BACKEND", "v1.1", JobGuideDocumentStatus.DRAFT);
        draft.completeIndexing("fake", "test-model", 64);
        GuideChunkReplaceRequest request = mock(GuideChunkReplaceRequest.class);
        GuideChunkRequest replacement = chunk(0);
        when(request.getChunks()).thenReturn(List.of(replacement));
        when(guideRepository.findWithLockByGuideId(11L)).thenReturn(Optional.of(draft));

        guideService.replaceDraftChunks(11L, request);

        InOrder order = inOrder(chunkRepository);
        order.verify(chunkRepository).deleteByGuide_GuideId(11L);
        order.verify(chunkRepository).flush();
        order.verify(chunkRepository).saveAll(any());
        assertThat(draft.getIndexingStatus()).isEqualTo(GuideIndexingStatus.NOT_INDEXED);
    }

    @Test
    void returnsReviewChunksInRepositoryOrderWithoutEmbeddingReference() {
        JobGuideChunk first = JobGuideChunk.builder()
                .guide(guide(11L, "BACKEND", "v1.1", JobGuideDocumentStatus.DRAFT))
                .chunkIndex(0)
                .title("첫째")
                .content("본문1")
                .contentSummary("요약1")
                .embeddingRef("vector-secret-1")
                .build();
        JobGuideChunk second = JobGuideChunk.builder()
                .guide(first.getGuide())
                .chunkIndex(1)
                .title("둘째")
                .content("본문2")
                .contentSummary("요약2")
                .embeddingRef("vector-secret-2")
                .build();
        when(guideRepository.existsById(11L)).thenReturn(true);
        when(chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(11L))
                .thenReturn(List.of(first, second));

        var responses = guideService.getChunks(11L);

        assertThat(responses).extracting("chunkIndex").containsExactly(0, 1);
        assertThat(responses).extracting("title").containsExactly("첫째", "둘째");
        assertThat(responses.getFirst().getClass().getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("embeddingRef");
    }

    private JobGuideDocument guide(
            Long id, String code, String version, JobGuideDocumentStatus status
    ) {
        JobGuideDocument guide = JobGuideDocument.builder()
                .guideCode(code)
                .scopeType(GuideScopeType.GLOBAL_COMMON)
                .title("가이드")
                .sourceType(JobGuideDocumentSourceType.DIRECT_INPUT)
                .version(version)
                .createdBy(admin)
                .applicableScope("전체")
                .evaluationFocus(List.of())
                .evidenceRules(List.of())
                .questionDirection(List.of())
                .avoidQuestions(List.of())
                .build();
        ReflectionTestUtils.setField(guide, "guideId", id);
        ReflectionTestUtils.setField(guide, "status", status);
        return guide;
    }

    private GuideChunkRequest chunk(int index) {
        GuideChunkRequest chunk = mock(GuideChunkRequest.class);
        when(chunk.getChunkIndex()).thenReturn(index);
        when(chunk.getContent()).thenReturn("내용 " + index);
        return chunk;
    }
}
