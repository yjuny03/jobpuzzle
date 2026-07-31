package com.example.jobpuzzle.interview.service;

import com.example.jobpuzzle.evaluation.entity.WeaknessTagResolveStatus;
import com.example.jobpuzzle.evaluation.repository.AnswerEvaluationRepository;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagLogRepository;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagStatusRepository;
import com.example.jobpuzzle.evaluation.service.AnswerEvaluationService;
import com.example.jobpuzzle.evaluation.service.WeaknessTagNormalizer;
import com.example.jobpuzzle.analysis.service.CompanyFitQuestionSetQueryService;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.interview.dto.SessionCreateRequest;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.InterviewSession;
import com.example.jobpuzzle.interview.entity.QuestionSet;
import com.example.jobpuzzle.interview.entity.QuestionSetStatus;
import com.example.jobpuzzle.interview.repository.FollowUpQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewMessageRepository;
import com.example.jobpuzzle.interview.repository.InterviewQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionRepository;
import com.example.jobpuzzle.interview.repository.QuestionSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewSessionLifecycleServiceTest {

    @Mock private QuestionSetRepository questionSetRepository;
    @Mock private InterviewQuestionRepository interviewQuestionRepository;
    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private InterviewSessionQuestionRepository sessionQuestionRepository;
    @Mock private InterviewMessageRepository interviewMessageRepository;
    @Mock private FollowUpQuestionRepository followUpQuestionRepository;
    @Mock private CompanyFitQuestionSetQueryService companyFitQuestionSetQueryService;
    @Mock private AnswerEvaluationService answerEvaluationService;
    @Mock private AnswerEvaluationRepository answerEvaluationRepository;
    @Mock private WeaknessTagStatusRepository weaknessTagStatusRepository;
    @Mock private WeaknessTagLogRepository weaknessTagLogRepository;
    @Mock private WeaknessTagNormalizer weaknessTagNormalizer;

    @InjectMocks private InterviewSessionService service;

    @Test
    void companyFitModeIsUnavailableWhenEveryActiveQuestionSetWasAlreadyConsumed() {
        QuestionSet consumed = mock(QuestionSet.class);
        when(consumed.getQuestionSetId()).thenReturn(41L);
        when(questionSetRepository.findByUser_UserIdAndInterviewModeAndStatus(
                7L, InterviewSessionMode.COMPANY_FIT, QuestionSetStatus.ACTIVE
        )).thenReturn(List.of(consumed));
        when(interviewSessionRepository.existsByQuestionSet_QuestionSetId(41L)).thenReturn(true);
        when(weaknessTagStatusRepository.existsByUser_UserIdAndStatus(
                7L, WeaknessTagResolveStatus.UNRESOLVED
        )).thenReturn(false);

        var companyFit = service.getAvailableModes(7L).getModes().stream()
                .filter(mode -> mode.getMode() == InterviewSessionMode.COMPANY_FIT)
                .findFirst()
                .orElseThrow();

        assertThat(companyFit.isAvailable()).isFalse();
    }

    @Test
    void companyFitModeIsAvailableWhenAtLeastOneActiveQuestionSetIsUnused() {
        QuestionSet consumed = mock(QuestionSet.class);
        QuestionSet unused = mock(QuestionSet.class);
        when(consumed.getQuestionSetId()).thenReturn(41L);
        when(unused.getQuestionSetId()).thenReturn(42L);
        when(questionSetRepository.findByUser_UserIdAndInterviewModeAndStatus(
                7L, InterviewSessionMode.COMPANY_FIT, QuestionSetStatus.ACTIVE
        )).thenReturn(List.of(consumed, unused));
        when(interviewSessionRepository.existsByQuestionSet_QuestionSetId(41L)).thenReturn(true);
        when(interviewSessionRepository.existsByQuestionSet_QuestionSetId(42L)).thenReturn(false);

        var companyFit = service.getAvailableModes(7L).getModes().stream()
                .filter(mode -> mode.getMode() == InterviewSessionMode.COMPANY_FIT)
                .findFirst()
                .orElseThrow();

        assertThat(companyFit.isAvailable()).isTrue();
    }

    @Test
    void canceledOrCompletedSessionStillPreventsQuestionSetReuse() {
        QuestionSet questionSet = mock(QuestionSet.class);
        when(questionSet.getQuestionSetId()).thenReturn(41L);
        when(questionSet.isActive()).thenReturn(true);
        when(questionSetRepository.findByQuestionSetIdAndUser_UserId(41L, 7L))
                .thenReturn(Optional.of(questionSet));
        when(interviewSessionRepository.existsByQuestionSet_QuestionSetId(41L)).thenReturn(true);
        SessionCreateRequest request = new SessionCreateRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 41L);
        ReflectionTestUtils.setField(request, "selectedQuestionIds", List.of(101L));

        assertThatThrownBy(() -> service.createSession(7L, request))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.INTERVIEW_SESSION_ALREADY_EXISTS);

        verify(interviewSessionRepository).existsByQuestionSet_QuestionSetId(41L);
    }

    @Test
    void completedSessionCannotBeStartedAgain() {
        InterviewSession completed = mock(InterviewSession.class);
        when(completed.isEditable()).thenReturn(false);
        when(interviewSessionRepository.findBySessionIdAndUser_UserIdAndDeletedAtIsNull(90L, 7L))
                .thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.startSession(7L, 90L))
                .isInstanceOf(CustomException.class)
                .extracting(error -> ((CustomException) error).getErrorCode())
                .isEqualTo(ErrorCode.SESSION_NOT_EDITABLE);
    }
}
