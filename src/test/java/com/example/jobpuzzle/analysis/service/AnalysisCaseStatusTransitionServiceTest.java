package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseStatus;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisCaseStatusTransitionServiceTest {
    private static final long USER_ID = 1L;
    private static final long CASE_ID = 10L;

    @Mock private AnalysisCaseRepository analysisCaseRepository;
    private AnalysisCaseStatusTransitionService service;
    private AnalysisCase analysisCase;

    @BeforeEach
    void setUp() {
        service = new AnalysisCaseStatusTransitionService(analysisCaseRepository);
        analysisCase = AnalysisCase.builder().user(null).jobCategory(null).build();
        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.INPUT_CONFIRMED);
        when(analysisCaseRepository.findWithLockByAnalysisCaseIdAndUser_UserId(CASE_ID, USER_ID))
                .thenReturn(Optional.of(analysisCase));
    }

    @Test
    void startsConfirmedCaseAndRestartsFailedCase() {
        assertThat(service.startOrRestart(USER_ID, CASE_ID)).isTrue();
        assertThat(analysisCase.getStatus()).isEqualTo(AnalysisCaseStatus.ANALYZING);

        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.FAILED);
        assertThat(service.startOrRestart(USER_ID, CASE_ID)).isTrue();
        assertThat(analysisCase.getStatus()).isEqualTo(AnalysisCaseStatus.ANALYZING);
    }

    @Test
    void leavesConcurrentAnalyzingCaseUntouched() {
        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.ANALYZING);

        assertThat(service.startOrRestart(USER_ID, CASE_ID)).isFalse();
        assertThat(analysisCase.getStatus()).isEqualTo(AnalysisCaseStatus.ANALYZING);
    }

    @Test
    void marksOnlyAnalyzingCaseAsFailed() {
        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.ANALYZING);

        service.failIfAnalyzing(USER_ID, CASE_ID);

        assertThat(analysisCase.getStatus()).isEqualTo(AnalysisCaseStatus.FAILED);
        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.COMPLETED);
        service.failIfAnalyzing(USER_ID, CASE_ID);
        assertThat(analysisCase.getStatus()).isEqualTo(AnalysisCaseStatus.COMPLETED);
    }

    @Test
    void rejectsCompletedCaseRestart() {
        ReflectionTestUtils.setField(analysisCase, "status", AnalysisCaseStatus.COMPLETED);

        assertThatThrownBy(() -> service.startOrRestart(USER_ID, CASE_ID))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.ANALYSIS_CASE_NOT_READY);
    }
}
