package com.example.jobpuzzle.admin.service;

import com.example.jobpuzzle.admin.dto.AdminGuideCreateRequest;
import com.example.jobpuzzle.ai.log.AiCallLogRepository;
import com.example.jobpuzzle.document.extraction.PdfTextExtractor;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.entity.GuideScopeType;
import com.example.jobpuzzle.guide.entity.JobGuideChunk;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentStatus;
import com.example.jobpuzzle.guide.repository.GuideContextResultRepository;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import com.example.jobpuzzle.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceGuideTest {

    @Mock private UserRepository userRepository;
    @Mock private JobCategoryRepository jobCategoryRepository;
    @Mock private GuideContextResultRepository guideContextResultRepository;
    @Mock private AiCallLogRepository aiCallLogRepository;
    @Mock private JobGuideDocumentRepository guideRepository;
    @Mock private JobGuideChunkRepository chunkRepository;
    @Mock private PdfTextExtractor pdfTextExtractor;
    @InjectMocks private AdminService adminService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(admin.getLoginId()).thenReturn("admin");
        when(chunkRepository.countByGuide_GuideId(any())).thenReturn(0L);
    }

    @Test
    void newGuideUsesInitialVersionAndPipelineInitialStates() {
        AdminGuideCreateRequest request = request("BACKEND", GuideScopeType.GLOBAL_COMMON);
        when(guideRepository.existsByGuideCode("BACKEND")).thenReturn(false);
        when(guideRepository.save(any())).thenAnswer(invocation -> {
            JobGuideDocument saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "guideId", 1L);
            return saved;
        });

        var response = adminService.createGuide(request, null, admin);

        assertThat(response.getVersion()).isEqualTo("v1.0");
        assertThat(response.getStatus()).isEqualTo(JobGuideDocumentStatus.DRAFT);
        assertThat(response.getIndexingStatus().name()).isEqualTo("NOT_INDEXED");
        verify(chunkRepository).saveAll(argThat(chunks -> {
            List<JobGuideChunk> savedChunks = new java.util.ArrayList<>();
            chunks.forEach(savedChunks::add);
            return savedChunks.size() == 1
                    && "가이드 원문".equals(savedChunks.get(0).getContent());
        }));
    }

    @Test
    void nextVersionInheritsLineageScopeAndIgnoresRequestedScopeChange() {
        JobGuideDocument previous = guide(10L, "BACKEND", "v1.0");
        AdminGuideCreateRequest request = request(null, GuideScopeType.PARENT_CATEGORY);
        when(request.getScopeMainCategory()).thenReturn("디자인");
        when(guideRepository.findWithLockByGuideId(10L)).thenReturn(Optional.of(previous));
        when(guideRepository.existsByPreviousGuide_GuideId(10L)).thenReturn(false);
        when(guideRepository.existsByGuideCodeAndVersion("BACKEND", "v1.1")).thenReturn(false);
        when(guideRepository.save(any())).thenAnswer(invocation -> {
            JobGuideDocument saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "guideId", 11L);
            return saved;
        });

        adminService.createGuideVersion(10L, request, null, admin);

        ArgumentCaptor<JobGuideDocument> captor = ArgumentCaptor.forClass(JobGuideDocument.class);
        verify(guideRepository).save(captor.capture());
        JobGuideDocument next = captor.getValue();
        assertThat(next.getPreviousGuide()).isSameAs(previous);
        assertThat(next.getGuideCode()).isEqualTo("BACKEND");
        assertThat(next.getVersion()).isEqualTo("v1.1");
        assertThat(next.getScopeType()).isEqualTo(GuideScopeType.GLOBAL_COMMON);
        assertThat(next.getScopeMainCategory()).isNull();
    }

    @Test
    void cannotCreateBranchFromOldVersion() {
        JobGuideDocument previous = guide(10L, "BACKEND", "v1.0");
        when(guideRepository.findWithLockByGuideId(10L)).thenReturn(Optional.of(previous));
        when(guideRepository.existsByPreviousGuide_GuideId(10L)).thenReturn(true);

        assertThatThrownBy(() ->
                adminService.createGuideVersion(
                        10L, request(null, GuideScopeType.GLOBAL_COMMON), null, admin))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUIDE_NOT_LATEST_VERSION);
        verify(guideRepository, never()).save(any());
    }

    @Test
    void rejectsDirectInputGuideWithoutSourceBeforeSavingDocument() {
        AdminGuideCreateRequest request = request("EMPTY", GuideScopeType.GLOBAL_COMMON);
        when(request.getSourceText()).thenReturn(" ");
        when(guideRepository.existsByGuideCode("EMPTY")).thenReturn(false);

        assertThatThrownBy(() -> adminService.createGuide(request, null, admin))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUIDE_SOURCE_REQUIRED);
        verify(guideRepository, never()).save(any());
    }

    @Test
    void rejectsPdfGuideWithoutFileBeforeSavingDocument() {
        AdminGuideCreateRequest request = request("PDF", GuideScopeType.GLOBAL_COMMON);
        when(request.getSourceType()).thenReturn(JobGuideDocumentSourceType.PDF);
        MultipartFile emptyFile = mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);
        when(guideRepository.existsByGuideCode("PDF")).thenReturn(false);

        assertThatThrownBy(() -> adminService.createGuide(request, emptyFile, admin))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GUIDE_SOURCE_REQUIRED);
        verify(guideRepository, never()).save(any());
    }

    @Test
    void allowsGuideRegistrationWithoutOptionalQuestionInstructions() {
        AdminGuideCreateRequest request = request("PDF-ONLY", GuideScopeType.GLOBAL_COMMON);
        when(request.getApplicableScope()).thenReturn(null);
        when(request.getEvaluationFocus()).thenReturn(null);
        when(request.getEvidenceRules()).thenReturn(null);
        when(request.getQuestionDirection()).thenReturn(null);
        when(request.getAvoidQuestions()).thenReturn(null);
        when(guideRepository.existsByGuideCode("PDF-ONLY")).thenReturn(false);
        when(guideRepository.save(any())).thenAnswer(invocation -> {
            JobGuideDocument saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "guideId", 2L);
            return saved;
        });

        adminService.createGuide(request, null, admin);

        ArgumentCaptor<JobGuideDocument> guide = ArgumentCaptor.forClass(JobGuideDocument.class);
        verify(guideRepository).save(guide.capture());
        assertThat(guide.getValue().getApplicableScope()).isEmpty();
        assertThat(guide.getValue().getEvaluationFocus()).isNull();
    }

    @Test
    void rejectsGuideDeactivationWithoutAdminAuthority() {
        User member = mock(User.class);
        when(member.getRole()).thenReturn(UserRole.USER);

        assertThatThrownBy(() -> adminService.deactivateGuide(1L, member))
                .isInstanceOf(AccessDeniedException.class);
        verify(guideRepository, never()).findById(any());
    }

    private AdminGuideCreateRequest request(String code, GuideScopeType scopeType) {
        AdminGuideCreateRequest request = mock(AdminGuideCreateRequest.class);
        when(request.getGuideCode()).thenReturn(code);
        when(request.getTitle()).thenReturn("백엔드 가이드");
        when(request.getScopeType()).thenReturn(scopeType);
        when(request.getApplicableScope()).thenReturn("백엔드 지원자");
        when(request.getEvaluationFocus()).thenReturn(List.of());
        when(request.getEvidenceRules()).thenReturn(List.of());
        when(request.getQuestionDirection()).thenReturn(List.of());
        when(request.getAvoidQuestions()).thenReturn(List.of());
        when(request.getSourceType()).thenReturn(JobGuideDocumentSourceType.DIRECT_INPUT);
        when(request.getSourceText()).thenReturn("가이드 원문");
        return request;
    }

    private JobGuideDocument guide(Long id, String code, String version) {
        JobGuideDocument guide = JobGuideDocument.builder()
                .guideCode(code)
                .scopeType(GuideScopeType.GLOBAL_COMMON)
                .title("기존 가이드")
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
        ReflectionTestUtils.setField(guide, "status", JobGuideDocumentStatus.ACTIVE);
        return guide;
    }
}
