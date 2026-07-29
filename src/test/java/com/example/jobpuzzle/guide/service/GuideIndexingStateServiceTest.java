package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.guide.entity.*;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuideIndexingStateServiceTest {

    @Mock private JobGuideDocumentRepository guideRepository;
    @Mock private JobGuideChunkRepository chunkRepository;
    @InjectMocks private GuideIndexingStateService service;

    @Test
    void beginAndCompleteFreezeChunkReferencesAndEmbeddingContract() {
        JobGuideDocument guide = draft(1L);
        guide.markManuallyReadyForReview();
        JobGuideChunk chunk = JobGuideChunk.builder()
                .guide(guide).chunkIndex(0).title("기준").content("내용").contentSummary("요약").build();
        ReflectionTestUtils.setField(chunk, "chunkId", 10L);
        when(guideRepository.findWithLockByGuideId(1L)).thenReturn(Optional.of(guide));
        when(guideRepository.existsByPreviousGuide_GuideId(1L)).thenReturn(false);
        when(chunkRepository.findByGuide_GuideIdOrderByChunkIndexAsc(1L))
                .thenReturn(List.of(chunk));

        GuideIndexingLease lease = service.begin(1L);
        service.complete(
                1L, Map.of(10L, "guide-index:10"),
                "openai", "text-embedding-3-small", 1536);

        assertThat(lease.documents()).hasSize(1);
        assertThat(chunk.getEmbeddingRef()).isEqualTo("guide-index:10");
        assertThat(guide.getIndexingStatus()).isEqualTo(GuideIndexingStatus.INDEXED);
        assertThat(guide.getEmbeddingModel()).isEqualTo("text-embedding-3-small");
        assertThat(guide.getEmbeddingDimension()).isEqualTo(1536);
    }

    private JobGuideDocument draft(Long id) {
        JobGuideDocument guide = JobGuideDocument.builder()
                .guideCode("BACKEND")
                .scopeType(GuideScopeType.GLOBAL_COMMON)
                .title("가이드")
                .sourceType(JobGuideDocumentSourceType.DIRECT_INPUT)
                .version("v1.0")
                .createdBy(mock(User.class))
                .applicableScope("전체")
                .evaluationFocus(List.of())
                .evidenceRules(List.of())
                .questionDirection(List.of())
                .avoidQuestions(List.of())
                .build();
        ReflectionTestUtils.setField(guide, "guideId", id);
        return guide;
    }
}
