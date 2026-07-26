package com.example.jobpuzzle.analysis.rag.service;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.analysis.entity.*;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchRequest;
import com.example.jobpuzzle.analysis.rag.dto.AnalysisMaterialChunkSearchResult;
import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalChunk;
import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalResult;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalCorpusType;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalStatus;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalChunkRepository;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalResultRepository;
import com.example.jobpuzzle.analysis.rag.search.VectorSearchPort;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import com.example.jobpuzzle.analysis.repository.JobPostingAnalysisRepository;
import com.example.jobpuzzle.analysis.service.AnalysisMaterialChunkService;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementRetrievalServiceTest {

    private static final long USER_ID = 10L;
    private static final long SNAPSHOT_ID = 20L;

    @Mock private AnalysisInputSnapshotRepository snapshotRepository;
    @Mock private JobPostingAnalysisRepository jobPostingAnalysisRepository;
    @Mock private AnalysisMaterialChunkRepository materialChunkRepository;
    @Mock private RequirementRetrievalResultRepository retrievalResultRepository;
    @Mock private RequirementRetrievalChunkRepository retrievalChunkRepository;
    @Mock private VectorSearchPort vectorSearchPort;

    private RequirementRetrievalService service;
    private AnalysisInputSnapshot snapshot;
    private JobPostingAnalysis posting;

    @BeforeEach
    void setUp() {
        service = new RequirementRetrievalService(snapshotRepository, jobPostingAnalysisRepository, materialChunkRepository,
                retrievalResultRepository, retrievalChunkRepository, vectorSearchPort);
        snapshot = snapshot();
        posting = validPosting(List.of(requirement("required-1", "Spring Boot")), List.of(requirement("preferred-1", "Docker")));
        // 테스트별 검증 경로가 달라 공통 fixture 값은 필요한 경우에만 소비된다.
        lenient().when(snapshotRepository.findWithLockBySnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(snapshot));
        lenient().when(jobPostingAnalysisRepository.findBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(posting));
        lenient().when(vectorSearchPort.getEmbeddingProviderName()).thenReturn("fake");
        lenient().when(vectorSearchPort.getEmbeddingModelName()).thenReturn("fake-char-ngram-v1");
        lenient().when(retrievalResultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(retrievalChunkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsRequiredThenPreferredRetrievalsWithDeterministicQueriesAndCandidateScope() {
        when(vectorSearchPort.search(any())).thenReturn(List.of());

        List<RequirementRetrievalResult> results = service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3);

        assertThat(results).extracting(RequirementRetrievalResult::getRequirementId).containsExactly("required-1", "preferred-1");
        assertThat(results).extracting(RequirementRetrievalResult::getQueryText)
                .containsExactly("Spring Boot | 백엔드 | NEW", "Docker | 백엔드 | NEW");
        ArgumentCaptor<AnalysisMaterialChunkSearchRequest> requestCaptor = ArgumentCaptor.forClass(AnalysisMaterialChunkSearchRequest.class);
        verify(vectorSearchPort, times(2)).search(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).allSatisfy(request -> {
            assertThat(request.allowedDocumentTypes()).containsExactlyInAnyOrder(
                    UserDocumentType.RESUME, UserDocumentType.COVER_LETTER, UserDocumentType.PORTFOLIO, UserDocumentType.EXPERIENCE_NOTE);
            assertThat(request.chunkingVersion()).isEqualTo(AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION);
            assertThat(request.topK()).isEqualTo(3);
        });
    }

    @Test
    void persistsCompletedResultAndVerifiedChunkMetadata() {
        AnalysisMaterialChunk material = material(101L, 301L, "Spring Boot API");
        AnalysisMaterialChunkSearchResult searchResult = searchResult(material, 0.8d, 1);
        when(vectorSearchPort.search(any())).thenReturn(List.of(searchResult));
        stubScopedMaterials(List.of(material));

        service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3);

        ArgumentCaptor<RequirementRetrievalResult> resultCaptor = ArgumentCaptor.forClass(RequirementRetrievalResult.class);
        verify(retrievalResultRepository, times(2)).save(resultCaptor.capture());
        assertThat(resultCaptor.getAllValues()).allSatisfy(result -> assertThat(result.getStatus()).isEqualTo(RetrievalStatus.COMPLETED));
        ArgumentCaptor<Iterable<RequirementRetrievalChunk>> chunkCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(retrievalChunkRepository, times(2)).saveAll(chunkCaptor.capture());
    }

    @Test
    void reusesCompleteResultOnlyWhenItsStoredChunksRemainValid() {
        RequirementRetrievalResult existing = result("required-1", RequirementType.REQUIRED, "Spring Boot", RetrievalStatus.COMPLETED, 3);
        AnalysisMaterialChunk material = material(101L, 301L, "Spring Boot API");
        RequirementRetrievalChunk child = RequirementRetrievalChunk.create(existing, material, 1, 0.8d);
        when(retrievalResultRepository.findBySnapshot_SnapshotIdAndRequirementIdAndCorpusType(SNAPSHOT_ID, "required-1", RetrievalCorpusType.CANDIDATE))
                .thenReturn(Optional.of(existing));
        when(retrievalChunkRepository.findByRetrievalResult_RetrievalResultIdOrderByRankAsc(existing.getRetrievalResultId())).thenReturn(List.of(child));
        when(vectorSearchPort.search(any())).thenReturn(List.of());

        service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3);

        verify(vectorSearchPort, times(1)).search(any()); // preferred만 새로 검색하고 정상 required 결과는 재사용한다.
        verify(retrievalResultRepository, never()).delete(existing);
    }

    @Test
    void recreatesResultWhenStoredMaterialChunkIsDuplicated() {
        RequirementRetrievalResult existing = result("required-1", RequirementType.REQUIRED, "Spring Boot", RetrievalStatus.COMPLETED, 3);
        AnalysisMaterialChunk material = material(101L, 301L, "Spring Boot API");
        stubExistingChunks(existing, List.of(
                RequirementRetrievalChunk.create(existing, material, 1, 0.9d),
                RequirementRetrievalChunk.create(existing, material, 2, 0.8d)));

        service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3);

        verify(retrievalResultRepository).delete(existing);
    }

    @Test
    void recreatesResultWhenStoredScoreIsNaN() {
        RequirementRetrievalResult existing = result("required-1", RequirementType.REQUIRED, "Spring Boot", RetrievalStatus.COMPLETED, 3);
        stubExistingChunks(existing, List.of(RequirementRetrievalChunk.create(existing, material(101L, 301L, "Spring Boot API"), 1, Double.NaN)));

        service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3);

        verify(retrievalResultRepository).delete(existing);
    }

    @Test
    void recreatesResultWhenStoredScoreIsInfinite() {
        RequirementRetrievalResult existing = result("required-1", RequirementType.REQUIRED, "Spring Boot", RetrievalStatus.COMPLETED, 3);
        stubExistingChunks(existing, List.of(RequirementRetrievalChunk.create(existing, material(101L, 301L, "Spring Boot API"), 1, Double.POSITIVE_INFINITY)));

        service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3);

        verify(retrievalResultRepository).delete(existing);
    }

    @Test
    void recreatesResultWhenStoredScoresAreAscending() {
        RequirementRetrievalResult existing = result("required-1", RequirementType.REQUIRED, "Spring Boot", RetrievalStatus.COMPLETED, 3);
        stubExistingChunks(existing, List.of(
                RequirementRetrievalChunk.create(existing, material(101L, 301L, "Spring Boot API"), 1, 0.8d),
                RequirementRetrievalChunk.create(existing, material(102L, 302L, "Spring Boot Web"), 2, 0.9d)));

        service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3);

        verify(retrievalResultRepository).delete(existing);
    }

    @Test
    void recreatesIncompleteResultAfterDeletingChildrenAndFlushing() {
        RequirementRetrievalResult existing = result("required-1", RequirementType.REQUIRED, "Spring Boot", RetrievalStatus.COMPLETED, 3);
        AnalysisMaterialChunk material = material(101L, 301L, "Spring Boot API");
        RequirementRetrievalChunk invalidRank = RequirementRetrievalChunk.create(existing, material, 2, 0.8d);
        when(retrievalResultRepository.findBySnapshot_SnapshotIdAndRequirementIdAndCorpusType(SNAPSHOT_ID, "required-1", RetrievalCorpusType.CANDIDATE))
                .thenReturn(Optional.of(existing));
        when(retrievalChunkRepository.findByRetrievalResult_RetrievalResultIdOrderByRankAsc(existing.getRetrievalResultId())).thenReturn(List.of(invalidRank));
        when(vectorSearchPort.search(any())).thenReturn(List.of());

        service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3);

        InOrder order = inOrder(retrievalChunkRepository, retrievalResultRepository);
        order.verify(retrievalChunkRepository).deleteByRetrievalResult_RetrievalResultId(existing.getRetrievalResultId());
        order.verify(retrievalResultRepository).delete(existing);
        order.verify(retrievalResultRepository).flush();
    }

    @Test
    void rejectsSearchResultThatCannotBeResolvedInsideUserSnapshotCandidateScope() {
        AnalysisMaterialChunk material = material(101L, 301L, "Spring Boot API");
        AnalysisMaterialChunkSearchResult outOfScope = searchResult(material, 0.8d, 1);
        when(vectorSearchPort.search(any())).thenReturn(List.of(outOfScope));
        stubScopedMaterials(List.of());

        assertThatThrownBy(() -> service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3))
                .isInstanceOf(IllegalArgumentException.class);
        verify(retrievalChunkRepository, never()).saveAll(any());
    }

    @Test
    void rejectsOtherUsersSnapshotBeforeReadingJson01() {
        User owner = mock(User.class);
        when(owner.getUserId()).thenReturn(99L);
        when(snapshot.getUser()).thenReturn(owner);

        assertThatThrownBy(() -> service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3))
                .isInstanceOf(CustomException.class);
        verifyNoInteractions(jobPostingAnalysisRepository);
    }

    @Test
    void rejectsDuplicateRequirementIdsAcrossRequiredAndPreferred() {
        posting = validPosting(List.of(requirement("same", "Spring")), List.of(requirement("same", "Docker")));
        when(jobPostingAnalysisRepository.findBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(posting));

        assertThatThrownBy(() -> service.getOrCreateCandidateRetrievals(USER_ID, SNAPSHOT_ID, 3))
                .isInstanceOf(CustomException.class);
        verify(vectorSearchPort, never()).search(any());
    }

    // 기존 result의 재사용 검증 실패 시 같은 requirement를 새 검색으로 복구하도록 공통 설정한다.
    private void stubExistingChunks(RequirementRetrievalResult existing, List<RequirementRetrievalChunk> chunks) {
        when(retrievalResultRepository.findBySnapshot_SnapshotIdAndRequirementIdAndCorpusType(SNAPSHOT_ID, "required-1", RetrievalCorpusType.CANDIDATE))
                .thenReturn(Optional.of(existing));
        when(retrievalChunkRepository.findByRetrievalResult_RetrievalResultIdOrderByRankAsc(existing.getRetrievalResultId())).thenReturn(chunks);
        when(vectorSearchPort.search(any())).thenReturn(List.of());
    }

    private void stubScopedMaterials(List<AnalysisMaterialChunk> materials) {
        when(materialChunkRepository.findByChunkIdInAndUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersion(
                anyCollection(), eq(USER_ID), eq(SNAPSHOT_ID), anyCollection(), eq(AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION)))
                .thenReturn(materials);
    }

    private AnalysisInputSnapshot snapshot() {
        AnalysisInputSnapshot value = mock(AnalysisInputSnapshot.class);
        User user = mock(User.class);
        JobCategory category = mock(JobCategory.class);
        lenient().when(value.getSnapshotId()).thenReturn(SNAPSHOT_ID);
        lenient().when(value.getUser()).thenReturn(user);
        lenient().when(user.getUserId()).thenReturn(USER_ID);
        lenient().when(value.getJobCategory()).thenReturn(category);
        lenient().when(category.getSubCategory()).thenReturn("백엔드");
        lenient().when(category.getMainCategory()).thenReturn("개발");
        lenient().when(category.getCareerLevel()).thenReturn(JobCategoryCareerLevel.NEW);
        return value;
    }

    private JobPostingAnalysis validPosting(List<JobPostingAnalysis.Requirement> required, List<JobPostingAnalysis.Requirement> preferred) {
        JobPostingAnalysis value = mock(JobPostingAnalysis.class);
        AiCallLog log = mock(AiCallLog.class);
        lenient().when(value.getRequirements()).thenReturn(required);
        lenient().when(value.getPreferred()).thenReturn(preferred);
        lenient().when(value.getAiCallLog()).thenReturn(log);
        lenient().when(log.getStatus()).thenReturn(AiCallLogStatus.SUCCEEDED);
        lenient().when(log.getValid()).thenReturn(true);
        return value;
    }

    private JobPostingAnalysis.Requirement requirement(String id, String text) {
        return JobPostingAnalysis.Requirement.builder().requirementId(id).text(text).build();
    }

    private RequirementRetrievalResult result(String requirementId, RequirementType type, String text, RetrievalStatus status, int topK) {
        RequirementRetrievalResult value = RequirementRetrievalResult.create(snapshot, USER_ID, requirementId, type, text,
                RetrievalCorpusType.CANDIDATE, text + " | 백엔드 | NEW", queryHash(text + " | 백엔드 | NEW", topK), "fake", "fake-char-ngram-v1", topK, status);
        ReflectionTestUtils.setField(value, "retrievalResultId", 601L);
        return value;
    }

    private AnalysisMaterialChunk material(Long id, Long sourceId, String content) {
        AnalysisMaterialChunk value = mock(AnalysisMaterialChunk.class);
        AnalysisInputSnapshotSource source = mock(AnalysisInputSnapshotSource.class);
        lenient().when(value.getChunkId()).thenReturn(id);
        lenient().when(value.getSnapshot()).thenReturn(snapshot);
        lenient().when(value.getUserId()).thenReturn(USER_ID);
        lenient().when(value.getSnapshotSource()).thenReturn(source);
        lenient().when(source.getSnapshotSourceId()).thenReturn(sourceId);
        lenient().when(value.getDocumentId()).thenReturn(900L);
        lenient().when(value.getDocumentType()).thenReturn(UserDocumentType.RESUME);
        lenient().when(value.getPageStart()).thenReturn(1);
        lenient().when(value.getPageEnd()).thenReturn(1);
        lenient().when(value.getCharStart()).thenReturn(0);
        lenient().when(value.getCharEnd()).thenReturn(content.length());
        lenient().when(value.getChunkIndex()).thenReturn(0);
        lenient().when(value.getContent()).thenReturn(content);
        lenient().when(value.getChunkingVersion()).thenReturn(AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION);
        return value;
    }

    private AnalysisMaterialChunkSearchResult searchResult(AnalysisMaterialChunk material, double score, int rank) {
        return new AnalysisMaterialChunkSearchResult(material.getChunkId(), material.getSnapshotSource().getSnapshotSourceId(),
                material.getDocumentId(), material.getDocumentType(), material.getPageStart(), material.getPageEnd(),
                material.getCharStart(), material.getCharEnd(), material.getChunkIndex(), material.getContent(), score, rank);
    }

    // 서비스와 같은 계약 문자열을 hash로 만들어 정상 재사용 fixture를 구성한다.
    private String queryHash(String queryText, int topK) {
        String types = Arrays.stream(new UserDocumentType[]{UserDocumentType.RESUME, UserDocumentType.COVER_LETTER,
                        UserDocumentType.PORTFOLIO, UserDocumentType.EXPERIENCE_NOTE})
                .map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(","));
        String value = queryText + "|fake|fake-char-ngram-v1|" + topK + "|" + types + "|"
                + AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
