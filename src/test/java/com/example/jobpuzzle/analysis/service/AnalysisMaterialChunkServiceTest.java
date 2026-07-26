package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshotSource;
import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalChunkRepository;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalResultRepository;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisMaterialChunkServiceTest {

    private static final String VERSION = AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION;
    private static final long USER_ID = 10L;
    private static final long SNAPSHOT_ID = 20L;
    private static final long SOURCE_ID = 30L;

    @Mock private AnalysisMaterialChunkRepository chunkRepository;
    @Mock private RequirementRetrievalChunkRepository retrievalChunkRepository;
    @Mock private RequirementRetrievalResultRepository retrievalResultRepository;
    private AnalysisMaterialChunkService chunkService;

    @BeforeEach
    void setUp() {
        chunkService = new AnalysisMaterialChunkService(chunkRepository, retrievalChunkRepository, retrievalResultRepository);
    }

    @Test
    void splitsMarkedDocumentWithoutCrossingPages() {
        stubChunkSave();
        String content = "[1페이지]\n첫 페이지 본문\n\n[2페이지]\n둘째 페이지 본문";

        List<AnalysisMaterialChunk> chunks = chunkService.getOrCreateChunks(source(content, DocumentVersionStatus.CONFIRMED), VERSION);

        assertThat(chunks).hasSize(2);
        assertThat(chunks).extracting(AnalysisMaterialChunk::getPageStart).containsExactly(1, 2);
        assertThat(chunks).extracting(AnalysisMaterialChunk::getContent).containsExactly("첫 페이지 본문", "둘째 페이지 본문");
    }

    @Test
    void treatsDirectInputWithoutPageMarkerAsSinglePage() {
        stubChunkSave();
        List<AnalysisMaterialChunk> chunks = chunkService.getOrCreateChunks(source("직접 입력한 경험 정리", DocumentVersionStatus.CONFIRMED), VERSION);

        assertThat(chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.getPageStart()).isEqualTo(1);
            assertThat(chunk.getPageEnd()).isEqualTo(1);
        });
    }

    @Test
    void splitsLongParagraphAtMaximumLength() {
        stubChunkSave();
        String content = "가".repeat(AnalysisMaterialChunkService.MAX_CHUNK_LENGTH + 5);

        List<AnalysisMaterialChunk> chunks = chunkService.getOrCreateChunks(source(content, DocumentVersionStatus.CONFIRMED), VERSION);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContent()).hasSize(AnalysisMaterialChunkService.MAX_CHUNK_LENGTH);
        assertThat(chunks.get(1).getContent()).hasSize(5);
    }

    @Test
    void storesContentAsExactOriginalSubstringWithEndExclusiveRange() {
        stubChunkSave();
        String content = "[1페이지]\n가나다라마바사";
        List<AnalysisMaterialChunk> chunks = chunkService.getOrCreateChunks(source(content, DocumentVersionStatus.CONFIRMED), VERSION);

        AnalysisMaterialChunk chunk = chunks.get(0);
        assertThat(chunk.getContent()).isEqualTo(content.substring(chunk.getCharStart(), chunk.getCharEnd()));
        assertThat(chunk.getCharEnd() - chunk.getCharStart()).isEqualTo(chunk.getContent().length());
        assertThat(content.charAt(chunk.getCharEnd() - 1)).isEqualTo(chunk.getContent().charAt(chunk.getContent().length() - 1));
    }

    @Test
    void reusesCompleteExistingChunksOnRepeatedCall() {
        stubChunkSave();
        AnalysisInputSnapshotSource source = source("재사용 대상", DocumentVersionStatus.CONFIRMED);
        List<AnalysisMaterialChunk> first = chunkService.getOrCreateChunks(source, VERSION);
        when(chunkRepository.existsBySnapshotSource_SnapshotSourceIdAndChunkingVersion(SOURCE_ID, VERSION)).thenReturn(true);
        when(chunkRepository.findBySnapshotSource_SnapshotSourceIdAndChunkingVersionOrderByChunkIndexAsc(SOURCE_ID, VERSION))
                .thenReturn(first);

        List<AnalysisMaterialChunk> reused = chunkService.getOrCreateChunks(source, VERSION);

        assertThat(reused).isSameAs(first);
        verify(chunkRepository, times(1)).saveAll(any());
        verify(chunkRepository, never()).deleteBySnapshotSource_SnapshotSourceIdAndChunkingVersion(anyLong(), anyString());
    }

    @Test
    void assignsContinuousChunkIndexesStartingAtZero() {
        stubChunkSave();
        String content = "가".repeat(AnalysisMaterialChunkService.MAX_CHUNK_LENGTH) + "\n\n"
                + "나".repeat(AnalysisMaterialChunkService.MAX_CHUNK_LENGTH);

        List<AnalysisMaterialChunk> chunks = chunkService.getOrCreateChunks(source(content, DocumentVersionStatus.CONFIRMED), VERSION);

        assertThat(chunks).extracting(AnalysisMaterialChunk::getChunkIndex).containsExactly(0, 1);
    }

    @Test
    void savesRepeatedSentenceChunksWithoutContentHashUniqueness() {
        stubChunkSave();
        String repeated = "가".repeat(AnalysisMaterialChunkService.MAX_CHUNK_LENGTH);
        List<AnalysisMaterialChunk> chunks = chunkService.getOrCreateChunks(
                source(repeated + "\n\n" + repeated, DocumentVersionStatus.CONFIRMED), VERSION);

        assertThat(chunks).hasSize(2);
        assertThat(chunks).extracting(AnalysisMaterialChunk::getContentHash).containsOnly(chunks.get(0).getContentHash());
        assertThat(chunks).extracting(AnalysisMaterialChunk::getChunkIndex).containsExactly(0, 1);
    }

    @Test
    void rejectsUnconfirmedExtraction() {
        assertThatThrownBy(() -> chunkService.getOrCreateChunks(source("초안", DocumentVersionStatus.DRAFT), VERSION))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.EXTRACTION_NOT_CONFIRMED);
    }

    @Test
    void rejectsDocumentOwnedByAnotherUser() {
        assertThatThrownBy(() -> chunkService.getOrCreateChunks(source("타인 자료", DocumentVersionStatus.CONFIRMED, 99L), VERSION))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void deletesIncompleteExistingSetAndRegeneratesIt() {
        stubChunkSave();
        AnalysisInputSnapshotSource source = source("재생성 대상", DocumentVersionStatus.CONFIRMED);
        AnalysisMaterialChunk malformed = AnalysisMaterialChunk.create(source.getSnapshot(), source, source.getExtraction(), USER_ID,
                40L, UserDocumentType.EXPERIENCE_NOTE, 1, 1, 0, 1, 3, "재", "broken", VERSION);
        when(chunkRepository.existsBySnapshotSource_SnapshotSourceIdAndChunkingVersion(SOURCE_ID, VERSION)).thenReturn(true);
        when(chunkRepository.findBySnapshotSource_SnapshotSourceIdAndChunkingVersionOrderByChunkIndexAsc(SOURCE_ID, VERSION))
                .thenReturn(List.of(malformed));

        List<AnalysisMaterialChunk> regenerated = chunkService.getOrCreateChunks(source, VERSION);

        verify(chunkRepository).deleteBySnapshotSource_SnapshotSourceIdAndChunkingVersion(SOURCE_ID, VERSION);
        assertThat(regenerated).singleElement().extracting(AnalysisMaterialChunk::getChunkIndex).isEqualTo(0);
        assertThat(regenerated.get(0).getContentHash()).isNotEqualTo("broken");
    }

    @Test
    void removesOnlyRetrievalsReferencingRegeneratedSourceVersionBeforeDeletingChunks() {
        stubChunkSave();
        AnalysisInputSnapshotSource source = source("재생성 대상", DocumentVersionStatus.CONFIRMED);
        AnalysisMaterialChunk malformed = AnalysisMaterialChunk.create(source.getSnapshot(), source, source.getExtraction(), USER_ID,
                40L, UserDocumentType.EXPERIENCE_NOTE, 1, 1, 0, 1, 3, "재", "broken", VERSION);
        when(chunkRepository.existsBySnapshotSource_SnapshotSourceIdAndChunkingVersion(SOURCE_ID, VERSION)).thenReturn(true);
        when(chunkRepository.findBySnapshotSource_SnapshotSourceIdAndChunkingVersionOrderByChunkIndexAsc(SOURCE_ID, VERSION))
                .thenReturn(List.of(malformed));
        when(retrievalChunkRepository.findDistinctRetrievalResultIdsByMaterialChunkSnapshotSourceIdAndChunkingVersion(SOURCE_ID, VERSION))
                .thenReturn(List.of(601L));

        chunkService.getOrCreateChunks(source, VERSION);

        InOrder order = inOrder(retrievalChunkRepository, retrievalResultRepository, chunkRepository);
        order.verify(retrievalChunkRepository).deleteByRetrievalResult_RetrievalResultIdIn(List.of(601L));
        order.verify(retrievalResultRepository).deleteByRetrievalResultIdIn(List.of(601L));
        order.verify(retrievalResultRepository).flush();
        order.verify(chunkRepository).deleteBySnapshotSource_SnapshotSourceIdAndChunkingVersion(SOURCE_ID, VERSION);
        verify(retrievalChunkRepository, never())
                .findDistinctRetrievalResultIdsByMaterialChunkSnapshotSourceIdAndChunkingVersion(31L, VERSION);
    }

    @Test
    void producesSameHashesAndOrderForSameInput() {
        stubChunkSave();
        String content = "[1페이지]\n첫 문단\n\n둘 문단\n\n[2페이지]\n셋 문단";

        List<AnalysisMaterialChunk> first = chunkService.getOrCreateChunks(source(content, DocumentVersionStatus.CONFIRMED), VERSION);
        List<AnalysisMaterialChunk> second = chunkService.getOrCreateChunks(source(content, DocumentVersionStatus.CONFIRMED), VERSION);

        assertThat(second).extracting(AnalysisMaterialChunk::getChunkIndex)
                .containsExactlyElementsOf(first.stream().map(AnalysisMaterialChunk::getChunkIndex).toList());
        assertThat(second).extracting(AnalysisMaterialChunk::getContentHash)
                .containsExactlyElementsOf(first.stream().map(AnalysisMaterialChunk::getContentHash).toList());
    }

    private AnalysisInputSnapshotSource source(String content, DocumentVersionStatus versionStatus) {
        return source(content, versionStatus, USER_ID);
    }

    // 저장 경로를 검증하는 테스트에서만 stubbing해 예외 테스트의 불필요한 설정을 피한다.
    private void stubChunkSave() {
        when(chunkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AnalysisInputSnapshotSource source(String content, DocumentVersionStatus versionStatus, long documentUserId) {
        User snapshotUser = mock(User.class);
        when(snapshotUser.getUserId()).thenReturn(USER_ID);
        User documentUser = mock(User.class);
        when(documentUser.getUserId()).thenReturn(documentUserId);
        UserDocument document = UserDocument.builder().user(documentUser).documentType(UserDocumentType.EXPERIENCE_NOTE)
                .sourceType(UserDocumentSourceType.TEXT).displayName("경험 정리").keepOriginal(false).build();
        ReflectionTestUtils.setField(document, "documentId", 40L);
        DocumentExtraction extraction = DocumentExtraction.builder().document(document).versionStatus(versionStatus)
                .content(content).build();
        ReflectionTestUtils.setField(extraction, "extractionId", 50L);
        AnalysisInputSnapshot snapshot = AnalysisInputSnapshot.builder().user(snapshotUser).build();
        ReflectionTestUtils.setField(snapshot, "snapshotId", SNAPSHOT_ID);
        AnalysisInputSnapshotSource source = AnalysisInputSnapshotSource.builder().snapshot(snapshot).extraction(extraction)
                .documentType(UserDocumentType.EXPERIENCE_NOTE).build();
        ReflectionTestUtils.setField(source, "snapshotSourceId", SOURCE_ID);
        return source;
    }
}
