package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.guide.dto.GuidePreprocessingResult;
import com.example.jobpuzzle.guide.entity.*;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuidePreprocessingStateServiceTest {

    @Mock private JobGuideDocumentRepository guideRepository;
    @Mock private JobGuideChunkRepository chunkRepository;
    @InjectMocks private GuidePreprocessingStateService service;

    @Test
    void completesDraftWithSequentialChunksAndReviewReadyStatus() {
        JobGuideDocument guide = draft(1L);
        guide.startPreprocessing();
        when(guideRepository.findWithLockByGuideId(1L)).thenReturn(Optional.of(guide));
        when(guideRepository.existsByPreviousGuide_GuideId(1L)).thenReturn(false);
        GuidePreprocessingResult result = new GuidePreprocessingResult(
                "백엔드",
                List.of("직무 적합성"),
                List.of("사실 근거"),
                List.of("경험 확인"),
                List.of(),
                List.of(
                        new GuidePreprocessingResult.Chunk("첫째", "내용1", "요약1"),
                        new GuidePreprocessingResult.Chunk("둘째", "내용2", "요약2"))
        );

        service.complete(1L, "gpt-5.6-terra", result);

        ArgumentCaptor<List<JobGuideChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(chunkRepository).deleteByGuide_GuideId(1L);
        verify(chunkRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(JobGuideChunk::getChunkIndex)
                .containsExactly(0, 1);
        assertThat(guide.getPreprocessingStatus())
                .isEqualTo(GuidePreprocessingStatus.READY_FOR_REVIEW);
        assertThat(guide.getPreprocessingModel()).isEqualTo("gpt-5.6-terra");
        assertThat(guide.getApplicableScope()).isEqualTo("백엔드");
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
