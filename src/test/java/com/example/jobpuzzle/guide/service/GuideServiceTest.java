package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.GuideChunkReplaceRequest;
import com.example.jobpuzzle.guide.dto.GuideChunkRequest;
import com.example.jobpuzzle.guide.dto.GuideRegisterRequest;
import com.example.jobpuzzle.guide.dto.GuideVersionCreateRequest;
import com.example.jobpuzzle.guide.entity.*;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuideServiceTest {

    @Mock private JobGuideDocumentRepository guideRepository;
    @Mock private JobGuideChunkRepository chunkRepository;
    @Mock private JobCategoryRepository jobCategoryRepository;
    @InjectMocks private GuideService guideService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(chunkRepository.countByGuide_GuideId(any())).thenReturn(0L);
    }

    @Test
    void rejectsDuplicateGuideCodeWhenStartingLineage() {
        GuideRegisterRequest request = mock(GuideRegisterRequest.class);
        when(request.getGuideCode()).thenReturn("BACKEND");
        when(guideRepository.existsByGuideCode("BACKEND")).thenReturn(true);

        assertThatThrownBy(() -> guideService.registerGuideDocument(admin, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUIDE_CODE_DUPLICATED);
        verify(guideRepository, never()).save(any());
    }

    @Test
    void nextVersionInheritsCodeAndScopeAndLinksPreviousVersion() {
        JobGuideDocument previous = guide(10L, "BACKEND", "v1.0", JobGuideDocumentStatus.ACTIVE);
        GuideVersionCreateRequest request = versionRequest("v1.1");
        when(guideRepository.findWithLockByGuideId(10L)).thenReturn(Optional.of(previous));
        when(guideRepository.existsByPreviousGuide_GuideId(10L)).thenReturn(false);
        when(guideRepository.existsByGuideCodeAndVersion("BACKEND", "v1.1")).thenReturn(false);
        when(guideRepository.save(any())).thenAnswer(invocation -> {
            JobGuideDocument saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "guideId", 11L);
            return saved;
        });

        guideService.createNextVersion(admin, 10L, request);

        ArgumentCaptor<JobGuideDocument> captor = ArgumentCaptor.forClass(JobGuideDocument.class);
        verify(guideRepository).save(captor.capture());
        JobGuideDocument next = captor.getValue();
        assertThat(next.getGuideCode()).isEqualTo("BACKEND");
        assertThat(next.getScopeType()).isEqualTo(GuideScopeType.GLOBAL_COMMON);
        assertThat(next.getPreviousGuide()).isSameAs(previous);
        assertThat(next.getVersion()).isEqualTo("v1.1");
        assertThat(next.getStatus()).isEqualTo(JobGuideDocumentStatus.DRAFT);
    }

    @Test
    void cannotBranchVersionLineageFromNonLatestVersion() {
        JobGuideDocument previous = guide(10L, "BACKEND", "v1.0", JobGuideDocumentStatus.ACTIVE);
        when(guideRepository.findWithLockByGuideId(10L)).thenReturn(Optional.of(previous));
        when(guideRepository.existsByPreviousGuide_GuideId(10L)).thenReturn(true);

        assertThatThrownBy(() ->
                guideService.createNextVersion(admin, 10L, versionRequest("v1.1")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUIDE_NOT_LATEST_VERSION);
    }

    @Test
    void activatingLatestDraftDeactivatesPreviousActiveInSameScope() {
        JobGuideDocument previousActive =
                guide(10L, "BACKEND", "v1.0", JobGuideDocumentStatus.ACTIVE);
        JobGuideDocument latestDraft =
                guide(11L, "BACKEND", "v1.1", JobGuideDocumentStatus.DRAFT);
        latestDraft.markManuallyReadyForReview();
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

    private GuideVersionCreateRequest versionRequest(String version) {
        GuideVersionCreateRequest request = mock(GuideVersionCreateRequest.class);
        when(request.getTitle()).thenReturn("새 가이드");
        when(request.getSourceType()).thenReturn(JobGuideDocumentSourceType.DIRECT_INPUT);
        when(request.getVersion()).thenReturn(version);
        when(request.getApplicableScope()).thenReturn("전체");
        when(request.getEvaluationFocus()).thenReturn(List.of());
        when(request.getEvidenceRules()).thenReturn(List.of());
        when(request.getQuestionDirection()).thenReturn(List.of());
        when(request.getAvoidQuestions()).thenReturn(List.of());
        return request;
    }

    private GuideChunkRequest chunk(int index) {
        GuideChunkRequest chunk = mock(GuideChunkRequest.class);
        when(chunk.getChunkIndex()).thenReturn(index);
        when(chunk.getContent()).thenReturn("내용 " + index);
        return chunk;
    }
}
