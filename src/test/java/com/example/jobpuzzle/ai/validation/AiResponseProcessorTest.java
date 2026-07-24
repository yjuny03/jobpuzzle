package com.example.jobpuzzle.ai.validation;

import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiResponseProcessorTest {

    private final AiResponseProcessor processor = new AiResponseProcessor(new ObjectMapper(), new AnalysisSourceMarkerParser());
    private final AnalysisInputSnapshotContextSource jobPosting = source(
            UserDocumentType.JOB_POSTING, 10L, 20L,
            "[SOURCE extractionId=10 documentId=20][PAGE=1][SEGMENT=seg-001]\n검증 가능한 공고 근거"
    );
    private final AnalysisInputSnapshotContextSource resume = source(
            UserDocumentType.RESUME, 11L, 21L,
            "[SOURCE extractionId=11 documentId=21][PAGE=1][SEGMENT=seg-002]\n검증 가능한 이력서 근거"
    );

    @Test
    void parsesFullJsonCodeBlockAndValidatesEchoedSourceReference() {
        JobPostingAnalysisResult result = processor.parseJobPosting("""
                ```json
                %s
                ```
                """.formatted(validJobJson("10", "20", "JOB_POSTING", "1", "seg-001", "검증 가능한 공고 근거")), List.of(jobPosting));

        assertThat(result.getMainTasks()).hasSize(1);
    }

    @Test
    void rejectsMalformedJsonAndUnknownField() {
        assertFailure(() -> processor.parseJobPosting("{", List.of(jobPosting)), AiCallLogErrorType.RESPONSE_PARSE_FAILED);
        assertFailure(() -> processor.parseJobPosting(validJobJson("10", "20", "JOB_POSTING", "1", "seg-001", "검증 가능한 공고 근거")
                .replace("{", "{\"unexpected\":true,"), List.of(jobPosting)), AiCallLogErrorType.RESPONSE_PARSE_FAILED);
        assertFailure(() -> processor.parseJobPosting(validJobJson("10", "20", "UNKNOWN_TYPE", "1", "seg-001", "검증 가능한 공고 근거"), List.of(jobPosting)), AiCallLogErrorType.RESPONSE_PARSE_FAILED);
    }

    @Test
    void rejectsMissingTopLevelFieldAndDuplicateId() {
        assertFailure(() -> processor.parseJobPosting("{\"mainTasks\":[]}", List.of(jobPosting)), AiCallLogErrorType.RESPONSE_VALIDATION_FAILED);
        String duplicate = validJobJson("10", "20", "JOB_POSTING", "1", "seg-001", "검증 가능한 공고 근거")
                .replace("}],\"requirements\"", "},{\"itemId\":\"task-1\",\"text\":\"공고 업무\",\"sourceRefs\":[{\"extractionId\":10,\"documentId\":20,\"documentType\":\"JOB_POSTING\",\"pageNumber\":1,\"segmentId\":\"seg-001\",\"evidenceText\":\"검증 가능한 공고 근거\"}]}],\"requirements\"");
        assertFailure(() -> processor.parseJobPosting(duplicate, List.of(jobPosting)), AiCallLogErrorType.RESPONSE_VALIDATION_FAILED);
    }

    @Test
    void rejectsUnknownSourceAndSourceAttributeMismatch() {
        assertFailure(() -> processor.parseJobPosting(validJobJson("99", "20", "JOB_POSTING", "1", "seg-001", "검증 가능한 공고 근거"), List.of(jobPosting)), AiCallLogErrorType.SOURCE_REFERENCE_INVALID);
        assertFailure(() -> processor.parseJobPosting(validJobJson("10", "21", "JOB_POSTING", "1", "seg-001", "검증 가능한 공고 근거"), List.of(jobPosting)), AiCallLogErrorType.SOURCE_REFERENCE_INVALID);
        assertFailure(() -> processor.parseJobPosting(validJobJson("10", "20", "JOB_POSTING", "2", "seg-001", "검증 가능한 공고 근거"), List.of(jobPosting)), AiCallLogErrorType.SOURCE_REFERENCE_INVALID);
        assertFailure(() -> processor.parseJobPosting(validJobJson("10", "20", "JOB_POSTING", "1", "seg-x", "검증 가능한 공고 근거"), List.of(jobPosting)), AiCallLogErrorType.SOURCE_REFERENCE_INVALID);
        assertFailure(() -> processor.parseJobPosting(validJobJson("10", "20", "JOB_POSTING", "1", "seg-001", "없는 발췌"), List.of(jobPosting)), AiCallLogErrorType.SOURCE_REFERENCE_INVALID);
    }

    @Test
    void rejectsCandidateReferenceInJson01AndAvailableDocumentTypesMismatch() {
        assertFailure(() -> processor.parseJobPosting(validJobJson("11", "21", "RESUME", "1", "seg-002", "검증 가능한 이력서 근거"), List.of(resume)), AiCallLogErrorType.SOURCE_REFERENCE_INVALID);
        String candidate = """
                {"availableDocumentTypes":["JOB_POSTING"],"resume":{"experiences":[],"skills":[],"roles":[],"results":[]},"coverLetter":null,"portfolio":null,"experienceNote":null,"missingEvidence":[]}
                """;
        assertFailure(() -> processor.parseCandidateMaterial(candidate, List.of(resume)), AiCallLogErrorType.RESPONSE_VALIDATION_FAILED);
    }

    private void assertFailure(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, AiCallLogErrorType expected) {
        assertThatThrownBy(callable).isInstanceOf(AiProcessingException.class)
                .extracting(error -> ((AiProcessingException) error).getErrorType()).isEqualTo(expected);
    }

    private String validJobJson(String extractionId, String documentId, String documentType, String pageNumber, String segmentId, String evidenceText) {
        return """
                {"mainTasks":[{"itemId":"task-1","text":"공고 업무","sourceRefs":[{"extractionId":%s,"documentId":%s,"documentType":"%s","pageNumber":%s,"segmentId":"%s","evidenceText":"%s"}]}],"requirements":[],"preferred":[],"companyValues":[],"coreCompetencies":[],"conflicts":[],"missingEvidence":[]}
                """.formatted(extractionId, documentId, documentType, pageNumber, segmentId, evidenceText);
    }

    private AnalysisInputSnapshotContextSource source(UserDocumentType type, Long extractionId, Long documentId, String analysisText) {
        AnalysisInputSnapshotContextSource source = mock(AnalysisInputSnapshotContextSource.class);
        when(source.getDocumentType()).thenReturn(type);
        when(source.getExtractionId()).thenReturn(extractionId);
        when(source.getDocumentId()).thenReturn(documentId);
        when(source.getAnalysisText()).thenReturn(analysisText);
        return source;
    }
}
