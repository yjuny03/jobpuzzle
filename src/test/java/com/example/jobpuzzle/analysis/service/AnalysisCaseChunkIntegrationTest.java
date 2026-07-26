package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotResponse;
import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseSource;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseSourceRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotSourceRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentExtractionStatus;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.document.repository.DocumentExtractionRepository;
import com.example.jobpuzzle.document.repository.UserDocumentRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

/** confirmInput과 청크 저장이 하나의 MariaDB 트랜잭션으로 완료되는지 검증한다. */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("db-integration")
@Testcontainers(disabledWithoutDocker = true)
class AnalysisCaseChunkIntegrationTest {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("jobpuzzle_chunk_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void mariaDbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", MARIADB::getDriverClassName);
    }

    @Autowired private AnalysisCaseService analysisCaseService;
    @Autowired private AnalysisCaseRepository analysisCaseRepository;
    @Autowired private AnalysisCaseSourceRepository analysisCaseSourceRepository;
    @Autowired private AnalysisInputSnapshotRepository snapshotRepository;
    @Autowired private AnalysisInputSnapshotSourceRepository snapshotSourceRepository;
    @Autowired private AnalysisMaterialChunkRepository chunkRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JobCategoryRepository jobCategoryRepository;
    @Autowired private UserDocumentRepository userDocumentRepository;
    @Autowired private DocumentExtractionRepository extractionRepository;
    @SpyBean private AnalysisMaterialChunkService chunkService;

    @Test
    void confirmInputCreatesChunksForEverySavedSnapshotSource() {
        Fixture fixture = fixture("one", List.of(UserDocumentType.JOB_POSTING, UserDocumentType.RESUME));

        AnalysisInputSnapshotResponse response = confirm(fixture);

        assertThat(response.getSources()).hasSize(2);
        savedSnapshotSources(response.getSnapshotId()).forEach(source -> assertThat(chunkRepository
                .findBySnapshotSource_SnapshotSourceIdAndChunkingVersionOrderByChunkIndexAsc(
                        source.getSnapshotSourceId(), AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION)).isNotEmpty());
    }

    @Test
    void startsChunkIndexAtZeroForEachSnapshotSource() {
        Fixture fixture = fixture("two", List.of(UserDocumentType.JOB_POSTING, UserDocumentType.RESUME));

        AnalysisInputSnapshotResponse response = confirm(fixture);

        savedSnapshotSources(response.getSnapshotId()).forEach(source -> assertThat(chunkRepository
                .findBySnapshotSource_SnapshotSourceIdAndChunkingVersionOrderByChunkIndexAsc(
                        source.getSnapshotSourceId(), AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION))
                .extracting(AnalysisMaterialChunk::getChunkIndex).containsExactly(0));
    }

    @Test
    void storesOnlyChunksOwnedByTheSnapshotUser() {
        Fixture fixture = fixture("three", List.of(UserDocumentType.JOB_POSTING, UserDocumentType.RESUME));

        AnalysisInputSnapshotResponse response = confirm(fixture);
        List<AnalysisMaterialChunk> chunks = chunkRepository.findByUserIdAndSnapshot_SnapshotIdOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(
                fixture.user().getUserId(), response.getSnapshotId());

        assertThat(chunks).isNotEmpty().allSatisfy(chunk -> assertThat(chunk.getUserId()).isEqualTo(fixture.user().getUserId()));
    }

    @Test
    void rejectsUnconfirmedExtractionBeforeSnapshotAndChunkCreation() {
        Fixture fixture = fixture("four", List.of(UserDocumentType.JOB_POSTING, UserDocumentType.RESUME));
        fixture.extractions().get(1).supersede();
        extractionRepository.save(fixture.extractions().get(1));
        long snapshotCount = snapshotRepository.count();
        long snapshotSourceCount = snapshotSourceRepository.count();
        long chunkCount = chunkRepository.count();

        assertThatThrownBy(() -> confirm(fixture)).isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode()).isEqualTo(ErrorCode.EXTRACTION_NOT_CONFIRMED);

        assertThat(snapshotRepository.count()).isEqualTo(snapshotCount);
        assertThat(snapshotSourceRepository.count()).isEqualTo(snapshotSourceCount);
        assertThat(chunkRepository.count()).isEqualTo(chunkCount);
    }

    @Test
    void rollsBackSnapshotSourcesChunksAndCaseStatusWhenChunkCreationFails() {
        Fixture fixture = fixture("five", List.of(UserDocumentType.JOB_POSTING, UserDocumentType.RESUME));
        long snapshotSourceCount = snapshotSourceRepository.count();
        long chunkCount = chunkRepository.count();
        doThrow(new IllegalStateException("chunk generation failed"))
                .when(chunkService).getOrCreateChunks(
                        argThat(source -> source.getDocumentType() == UserDocumentType.RESUME),
                        eq(AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION));

        assertThatThrownBy(() -> confirm(fixture)).isInstanceOf(IllegalStateException.class);

        AnalysisCase reloaded = analysisCaseRepository.findById(fixture.analysisCase().getAnalysisCaseId()).orElseThrow();
        assertThat(reloaded.isDraft()).isTrue();
        assertThat(snapshotRepository.findByAnalysisCase_AnalysisCaseIdAndUser_UserId(
                fixture.analysisCase().getAnalysisCaseId(), fixture.user().getUserId())).isEmpty();
        assertThat(snapshotSourceRepository.count()).isEqualTo(snapshotSourceCount);
        assertThat(chunkRepository.count()).isEqualTo(chunkCount);
    }

    @Test
    void isolatesChunkQueriesByUserAndSnapshot() {
        Fixture first = fixture("six-a", List.of(UserDocumentType.JOB_POSTING, UserDocumentType.RESUME));
        Fixture second = fixture("six-b", List.of(UserDocumentType.JOB_POSTING, UserDocumentType.RESUME));
        AnalysisInputSnapshotResponse firstResponse = confirm(first);
        AnalysisInputSnapshotResponse secondResponse = confirm(second);

        List<AnalysisMaterialChunk> firstChunks = chunkRepository.findByUserIdAndSnapshot_SnapshotIdOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(
                first.user().getUserId(), firstResponse.getSnapshotId());

        assertThat(firstChunks).isNotEmpty().allSatisfy(chunk -> {
            assertThat(chunk.getUserId()).isEqualTo(first.user().getUserId());
            assertThat(chunk.getSnapshot().getSnapshotId()).isEqualTo(firstResponse.getSnapshotId());
        });
        assertThat(firstChunks).extracting(AnalysisMaterialChunk::getChunkId)
                .doesNotContainAnyElementsOf(chunkRepository.findByUserIdAndSnapshot_SnapshotIdOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(
                        second.user().getUserId(), secondResponse.getSnapshotId()).stream().map(AnalysisMaterialChunk::getChunkId).toList());
    }

    @Test
    void createsNoDuplicateChunksDuringOneNormalConfirmation() {
        Fixture fixture = fixture("seven", List.of(UserDocumentType.JOB_POSTING, UserDocumentType.RESUME));

        AnalysisInputSnapshotResponse response = confirm(fixture);

        savedSnapshotSources(response.getSnapshotId()).forEach(source -> assertThat(chunkRepository
                .findBySnapshotSource_SnapshotSourceIdAndChunkingVersionOrderByChunkIndexAsc(
                        source.getSnapshotSourceId(), AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION))
                .extracting(AnalysisMaterialChunk::getChunkIndex).containsExactly(0));
    }

    // DRAFT case와 CONFIRMED extraction을 실제 DB에 준비해 confirmInput의 저장 경로를 검증한다.
    private Fixture fixture(String suffix, List<UserDocumentType> documentTypes) {
        JobCategory category = jobCategoryRepository.save(JobCategory.builder()
                .mainCategory("개발-" + suffix).subCategory("백엔드").careerLevel(JobCategoryCareerLevel.NEW).build());
        User user = userRepository.save(User.createLocalUser("chunk-" + suffix, "encoded", suffix + "@example.com", "사용자", category));
        AnalysisCase analysisCase = analysisCaseRepository.save(AnalysisCase.builder().user(user).jobCategory(category).build());
        List<DocumentExtraction> extractions = documentTypes.stream().map(type -> {
            UserDocument document = userDocumentRepository.save(UserDocument.builder().user(user).documentType(type)
                    .sourceType(UserDocumentSourceType.TEXT).displayName(type.name() + " 자료").keepOriginal(false).build());
            DocumentExtraction extraction = extractionRepository.save(DocumentExtraction.builder().document(document)
                    .extractionStatus(DocumentExtractionStatus.SUCCESS).versionStatus(DocumentVersionStatus.CONFIRMED)
                    .content("[1페이지]\\n" + type.name() + " 확정 원문").pageCount(1).build());
            analysisCaseSourceRepository.save(AnalysisCaseSource.builder().analysisCase(analysisCase)
                    .extraction(extraction).documentType(type).build());
            return extraction;
        }).toList();
        return new Fixture(user, analysisCase, extractions);
    }

    // 실제 서비스 호출을 한 곳에 모아 모든 통합 검증이 동일한 confirmInput 경로를 사용한다.
    private AnalysisInputSnapshotResponse confirm(Fixture fixture) {
        return analysisCaseService.confirmInput(fixture.user().getUserId(), fixture.analysisCase().getAnalysisCaseId());
    }

    // 응답 DTO에 없는 snapshotSourceId를 실제 저장 Entity에서 읽어 source별 청크를 검증한다.
    private List<com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshotSource> savedSnapshotSources(Long snapshotId) {
        return snapshotSourceRepository.findBySnapshot_SnapshotIdOrderBySnapshotSourceIdAsc(snapshotId);
    }

    private record Fixture(User user, AnalysisCase analysisCase, List<DocumentExtraction> extractions) { }
}
