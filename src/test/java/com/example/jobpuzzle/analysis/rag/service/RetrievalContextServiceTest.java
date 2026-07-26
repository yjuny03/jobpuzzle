package com.example.jobpuzzle.analysis.rag.service;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.analysis.entity.*;
import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalChunk;
import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalResult;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalCorpusType;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalStatus;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalChunkRepository;
import com.example.jobpuzzle.analysis.rag.repository.RequirementRetrievalResultRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.JobPostingAnalysisRepository;
import com.example.jobpuzzle.analysis.service.AnalysisMaterialChunkService;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetrievalContextServiceTest {
    private static final long USER_ID = 10L;
    private static final long SNAPSHOT_ID = 20L;

    @Mock private AnalysisInputSnapshotRepository snapshotRepository;
    @Mock private JobPostingAnalysisRepository jobPostingRepository;
    @Mock private RequirementRetrievalResultRepository resultRepository;
    @Mock private RequirementRetrievalChunkRepository chunkRepository;
    private RetrievalContextService service;
    private AnalysisInputSnapshot snapshot;

    @BeforeEach
    void setUp() {
        service = new RetrievalContextService(snapshotRepository, jobPostingRepository, resultRepository, chunkRepository);
        snapshot = mock(AnalysisInputSnapshot.class);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(USER_ID);
        when(snapshot.getSnapshotId()).thenReturn(SNAPSHOT_ID);
        when(snapshot.getUser()).thenReturn(user);
        when(snapshotRepository.findById(SNAPSHOT_ID)).thenReturn(Optional.of(snapshot));
        JobPostingAnalysis posting = posting();
        when(jobPostingRepository.findBySnapshot_SnapshotId(SNAPSHOT_ID)).thenReturn(Optional.of(posting));
    }

    @Test
    void assemblesJson01RequiredThenPreferredOrderAndKeepsEmptyRequirement() {
        RequirementRetrievalResult preferred = result("preferred-1", RequirementType.PREFERRED, "Docker", RetrievalStatus.EMPTY, 602L);
        RequirementRetrievalResult required = result("required-1", RequirementType.REQUIRED, "Spring", RetrievalStatus.COMPLETED, 601L);
        AnalysisMaterialChunk material = material(101L, "Spring evidence");
        RequirementRetrievalChunk retrievedChunk = chunk(required, material, 1, 0.9d, 701L);
        when(resultRepository.findByUserIdAndSnapshot_SnapshotIdOrderByRetrievalResultIdAsc(USER_ID, SNAPSHOT_ID))
                .thenReturn(List.of(preferred, required));
        when(chunkRepository.findByRetrievalResult_RetrievalResultIdOrderByRankAsc(601L))
                .thenReturn(List.of(retrievedChunk));
        when(chunkRepository.findByRetrievalResult_RetrievalResultIdOrderByRankAsc(602L)).thenReturn(List.of());

        var context = service.getCandidateEvidenceContext(USER_ID, SNAPSHOT_ID);

        assertThat(context.evidence().requirements()).extracting(value -> value.requirementId())
                .containsExactly("required-1", "preferred-1");
        assertThat(context.evidence().requirements().get(0).chunks()).singleElement()
                .satisfies(value -> assertThat(value.extractionId()).isEqualTo(301L));
        assertThat(context.evidence().requirements().get(1).chunks()).isEmpty();
        assertThat(context.fingerprintMaterial()).contains("601|required-1|COMPLETED|hash-601", "701|1|0.9|content-101|" + AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION);
    }

    @Test
    void rejectsCompletedResultWithoutChunks() {
        RequirementRetrievalResult required = result("required-1", RequirementType.REQUIRED, "Spring", RetrievalStatus.COMPLETED, 601L);
        RequirementRetrievalResult preferred = result("preferred-1", RequirementType.PREFERRED, "Docker", RetrievalStatus.EMPTY, 602L);
        when(resultRepository.findByUserIdAndSnapshot_SnapshotIdOrderByRetrievalResultIdAsc(USER_ID, SNAPSHOT_ID)).thenReturn(List.of(required, preferred));
        when(chunkRepository.findByRetrievalResult_RetrievalResultIdOrderByRankAsc(anyLong())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getCandidateEvidenceContext(USER_ID, SNAPSHOT_ID)).isInstanceOf(CustomException.class);
    }

    @Test
    void rejectsAscendingScoresAndDuplicateMaterialChunks() {
        RequirementRetrievalResult required = result("required-1", RequirementType.REQUIRED, "Spring", RetrievalStatus.COMPLETED, 601L);
        RequirementRetrievalResult preferred = result("preferred-1", RequirementType.PREFERRED, "Docker", RetrievalStatus.EMPTY, 602L);
        AnalysisMaterialChunk material = material(101L, "Spring evidence");
        RequirementRetrievalChunk first = chunk(required, material, 1, 0.8d, 701L);
        RequirementRetrievalChunk second = chunk(required, material, 2, 0.9d, 702L);
        when(resultRepository.findByUserIdAndSnapshot_SnapshotIdOrderByRetrievalResultIdAsc(USER_ID, SNAPSHOT_ID)).thenReturn(List.of(required, preferred));
        when(chunkRepository.findByRetrievalResult_RetrievalResultIdOrderByRankAsc(601L)).thenReturn(List.of(
                first, second));

        assertThatThrownBy(() -> service.getCandidateEvidenceContext(USER_ID, SNAPSHOT_ID)).isInstanceOf(CustomException.class);
    }

    private JobPostingAnalysis posting() {
        JobPostingAnalysis posting = mock(JobPostingAnalysis.class);
        AiCallLog log = mock(AiCallLog.class);
        when(log.getStatus()).thenReturn(AiCallLogStatus.SUCCEEDED);
        when(log.getValid()).thenReturn(true);
        when(posting.getAiCallLog()).thenReturn(log);
        when(posting.getRequirements()).thenReturn(List.of(JobPostingAnalysis.Requirement.builder().requirementId("required-1").text("Spring").build()));
        when(posting.getPreferred()).thenReturn(List.of(JobPostingAnalysis.Requirement.builder().requirementId("preferred-1").text("Docker").build()));
        return posting;
    }

    private RequirementRetrievalResult result(String id, RequirementType type, String text, RetrievalStatus status, long resultId) {
        RequirementRetrievalResult result = RequirementRetrievalResult.create(snapshot, USER_ID, id, type, text,
                RetrievalCorpusType.CANDIDATE, text, "hash-" + resultId, "fake", "fake-v1", 3, status);
        ReflectionTestUtils.setField(result, "retrievalResultId", resultId);
        return result;
    }

    private RequirementRetrievalChunk chunk(RequirementRetrievalResult result, AnalysisMaterialChunk material, int rank, double score, long retrievalChunkId) {
        RequirementRetrievalChunk chunk = RequirementRetrievalChunk.create(result, material, rank, score);
        ReflectionTestUtils.setField(chunk, "retrievalChunkId", retrievalChunkId);
        return chunk;
    }

    private AnalysisMaterialChunk material(long id, String content) {
        AnalysisMaterialChunk material = mock(AnalysisMaterialChunk.class);
        DocumentExtraction extraction = mock(DocumentExtraction.class);
        when(extraction.getExtractionId()).thenReturn(301L);
        when(material.getChunkId()).thenReturn(id);
        when(material.getSnapshot()).thenReturn(snapshot);
        when(material.getUserId()).thenReturn(USER_ID);
        when(material.getExtraction()).thenReturn(extraction);
        when(material.getDocumentId()).thenReturn(401L);
        when(material.getDocumentType()).thenReturn(UserDocumentType.RESUME);
        when(material.getChunkingVersion()).thenReturn(AnalysisMaterialChunkService.CURRENT_CHUNKING_VERSION);
        when(material.getContent()).thenReturn(content);
        when(material.getContentHash()).thenReturn("content-" + id);
        when(material.getPageStart()).thenReturn(1); when(material.getPageEnd()).thenReturn(1);
        when(material.getCharStart()).thenReturn(0); when(material.getCharEnd()).thenReturn(content.length());
        return material;
    }
}
