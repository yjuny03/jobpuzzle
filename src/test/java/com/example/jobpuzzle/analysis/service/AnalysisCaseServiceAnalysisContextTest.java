package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContext;
import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshotSource;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseSourceRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotSourceRepository;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentExtractionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.document.service.DocumentExtractionService;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// JSON-03 내부 컨텍스트 조립(getAnalysisContext) 전용 테스트.
// 1단계라 마스킹은 없고, [SOURCE ...][PAGE=...][SEGMENT=...] 마커 조립과 페이지 분할만 검증한다
@ExtendWith(MockitoExtension.class)
class AnalysisCaseServiceAnalysisContextTest {

    @Mock
    private AnalysisCaseRepository analysisCaseRepository;
    @Mock
    private AnalysisCaseSourceRepository analysisCaseSourceRepository;
    @Mock
    private AnalysisInputSnapshotRepository analysisInputSnapshotRepository;
    @Mock
    private AnalysisInputSnapshotSourceRepository analysisInputSnapshotSourceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobCategoryRepository jobCategoryRepository;
    @Mock
    private DocumentExtractionService documentExtractionService;

    @InjectMocks
    private AnalysisCaseService analysisCaseService;

    @Test
    @DisplayName("페이지 마커를 세그먼트 경계로 나누고, 세그먼트 번호는 소스 구분 없이 이어서 증가한다")
    void buildsAnalysisTextWithMarkersPerPage() {
        JobCategory jobCategory = JobCategory.builder()
                .mainCategory("IT·개발").subCategory("백엔드 개발").careerLevel(JobCategoryCareerLevel.NEW)
                .build();
        AnalysisCase analysisCase = AnalysisCase.builder().user(null).jobCategory(jobCategory).build();
        AnalysisInputSnapshot snapshot = AnalysisInputSnapshot.builder()
                .analysisCase(analysisCase).user(null).jobCategory(jobCategory).build();

        UserDocument document1 = UserDocument.builder()
                .documentType(UserDocumentType.JOB_POSTING).sourceType(UserDocumentSourceType.PDF)
                .displayName("A사 백엔드 채용공고").build();
        ReflectionTestUtils.setField(document1, "documentId", 501L);
        DocumentExtraction extraction1 = DocumentExtraction.builder()
                .document(document1)
                .content("[1페이지]\nSpring Boot 기반 API 개발 경험을 요구합니다.\n\n[2페이지]\n두번째 페이지 내용입니다.\n\n")
                .majorVersion(1).minorVersion(0)
                .extractionStatus(DocumentExtractionStatus.SUCCESS)
                .build();
        ReflectionTestUtils.setField(extraction1, "extractionId", 701L);

        UserDocument document2 = UserDocument.builder()
                .documentType(UserDocumentType.RESUME).sourceType(UserDocumentSourceType.TEXT)
                .displayName("백엔드 이력서").build();
        ReflectionTestUtils.setField(document2, "documentId", 502L);
        DocumentExtraction extraction2 = DocumentExtraction.builder()
                .document(document2)
                .content("마커 없는 직접 입력 텍스트입니다.")
                .majorVersion(1).minorVersion(2)
                .extractionStatus(DocumentExtractionStatus.SUCCESS)
                .build();
        ReflectionTestUtils.setField(extraction2, "extractionId", 702L);

        AnalysisInputSnapshotSource source1 = AnalysisInputSnapshotSource.builder()
                .snapshot(snapshot).extraction(extraction1).documentType(UserDocumentType.JOB_POSTING).build();
        AnalysisInputSnapshotSource source2 = AnalysisInputSnapshotSource.builder()
                .snapshot(snapshot).extraction(extraction2).documentType(UserDocumentType.RESUME).build();

        when(analysisInputSnapshotRepository.findByAnalysisCase_AnalysisCaseIdAndUser_UserId(10L, 1L))
                .thenReturn(Optional.of(snapshot));
        when(analysisInputSnapshotSourceRepository.findBySnapshot_SnapshotIdOrderBySnapshotSourceIdAsc(null))
                .thenReturn(List.of(source1, source2));

        AnalysisInputSnapshotContext context = analysisCaseService.getAnalysisContext(1L, 10L);

        assertThat(context.getMainCategory()).isEqualTo("IT·개발");
        assertThat(context.getSources()).hasSize(2);

        assertThat(context.getSources().get(0).getAnalysisText()).isEqualTo(
                "[SOURCE extractionId=701 documentId=501][PAGE=1][SEGMENT=seg-001]\n"
                        + "Spring Boot 기반 API 개발 경험을 요구합니다.\n\n"
                        + "[SOURCE extractionId=701 documentId=501][PAGE=2][SEGMENT=seg-002]\n"
                        + "두번째 페이지 내용입니다."
        );

        assertThat(context.getSources().get(1).getAnalysisText()).isEqualTo(
                "[SOURCE extractionId=702 documentId=502][PAGE=1][SEGMENT=seg-003]\n"
                        + "마커 없는 직접 입력 텍스트입니다."
        );
    }
}
