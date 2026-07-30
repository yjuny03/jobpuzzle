package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.interview.entity.InterviewQuestion;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.QuestionSet;
import com.example.jobpuzzle.interview.entity.QuestionSetStatus;
import com.example.jobpuzzle.interview.repository.InterviewQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionRepository;
import com.example.jobpuzzle.interview.repository.QuestionSetRepository;
import com.example.jobpuzzle.evaluation.repository.WeaknessTagLogRepository;
import com.example.jobpuzzle.evaluation.service.WeaknessTagNormalizer;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationServiceTest {

    @Mock private QuestionSetRepository questionSetRepository;
    @Mock private InterviewQuestionRepository interviewQuestionRepository;
    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private WeaknessTagLogRepository weaknessTagLogRepository;
    @Mock private WeaknessTagNormalizer weaknessTagNormalizer;
    @InjectMocks private QuestionGenerationService service;

    @Test
    void returnsOnlyUnusedBasicAndWeaknessQuestionSets() {
        QuestionSet unusedBasic = questionSet(11L);
        QuestionSet consumedBasic = questionSet(12L);
        QuestionSet unusedWeakness = questionSet(13L);
        when(unusedBasic.getInterviewMode()).thenReturn(InterviewSessionMode.BASIC);
        when(unusedWeakness.getInterviewMode()).thenReturn(InterviewSessionMode.WEAKNESS_REVIEW);
        InterviewQuestion basicQuestion = mock(InterviewQuestion.class);
        InterviewQuestion weaknessQuestion = mock(InterviewQuestion.class);

        when(questionSetRepository.findByUser_UserIdAndInterviewModeAndStatus(
                7L, InterviewSessionMode.BASIC, QuestionSetStatus.ACTIVE
        )).thenReturn(List.of(unusedBasic, consumedBasic));
        when(questionSetRepository.findByUser_UserIdAndInterviewModeAndStatus(
                7L, InterviewSessionMode.WEAKNESS_REVIEW, QuestionSetStatus.ACTIVE
        )).thenReturn(List.of(unusedWeakness));
        when(interviewSessionRepository.existsByQuestionSet_QuestionSetId(11L)).thenReturn(false);
        when(interviewSessionRepository.existsByQuestionSet_QuestionSetId(12L)).thenReturn(true);
        when(interviewSessionRepository.existsByQuestionSet_QuestionSetId(13L)).thenReturn(false);
        when(interviewQuestionRepository.findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(11L))
                .thenReturn(List.of(basicQuestion));
        when(interviewQuestionRepository.findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(13L))
                .thenReturn(List.of(weaknessQuestion));

        var result = service.getUnusedGeneratedQuestionSets(7L);

        assertThat(result).extracting(value -> value.getQuestionSetId())
                .containsExactly(13L, 11L);
    }

    @Test
    void preparedBasicQuestionIncludesItsJobCategoryAndCareerLevel() {
        QuestionSet unusedBasic = questionSet(21L);
        JobCategory category = mock(JobCategory.class);
        InterviewQuestion question = mock(InterviewQuestion.class);
        when(unusedBasic.getInterviewMode()).thenReturn(InterviewSessionMode.BASIC);
        when(unusedBasic.getJobCategory()).thenReturn(category);
        when(unusedBasic.getCareerLevel()).thenReturn(JobCategoryCareerLevel.NEW);
        when(category.getMainCategory()).thenReturn("IT·개발");
        when(category.getSubCategory()).thenReturn("백엔드 개발");
        when(questionSetRepository.findByUser_UserIdAndInterviewModeAndStatus(
                7L, InterviewSessionMode.BASIC, QuestionSetStatus.ACTIVE
        )).thenReturn(List.of(unusedBasic));
        when(questionSetRepository.findByUser_UserIdAndInterviewModeAndStatus(
                7L, InterviewSessionMode.WEAKNESS_REVIEW, QuestionSetStatus.ACTIVE
        )).thenReturn(List.of());
        when(interviewSessionRepository.existsByQuestionSet_QuestionSetId(21L)).thenReturn(false);
        when(interviewQuestionRepository.findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(21L))
                .thenReturn(List.of(question));

        var result = service.getUnusedGeneratedQuestionSets(7L).get(0);

        assertThat(result.getMainCategory()).isEqualTo("IT·개발");
        assertThat(result.getSubCategory()).isEqualTo("백엔드 개발");
        assertThat(result.getCareerLevel()).isEqualTo("신입");
        assertThat(result.getDisplayTitle()).isEqualTo("IT·개발 · 백엔드 개발 · 신입");
    }

    @Test
    void preparedWeaknessQuestionUsesTheKoreanWeaknessName() {
        QuestionSet unusedWeakness = questionSet(31L);
        InterviewQuestion question = mock(InterviewQuestion.class);
        when(unusedWeakness.getInterviewMode()).thenReturn(InterviewSessionMode.WEAKNESS_REVIEW);
        when(unusedWeakness.getTargetWeaknessTag()).thenReturn("requirementConnection_weak");
        when(weaknessTagNormalizer.displayName("requirementConnection_weak"))
                .thenReturn("공고 요구사항 연결 부족");
        when(questionSetRepository.findByUser_UserIdAndInterviewModeAndStatus(
                7L, InterviewSessionMode.BASIC, QuestionSetStatus.ACTIVE
        )).thenReturn(List.of());
        when(questionSetRepository.findByUser_UserIdAndInterviewModeAndStatus(
                7L, InterviewSessionMode.WEAKNESS_REVIEW, QuestionSetStatus.ACTIVE
        )).thenReturn(List.of(unusedWeakness));
        when(interviewSessionRepository.existsByQuestionSet_QuestionSetId(31L)).thenReturn(false);
        when(interviewQuestionRepository.findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(31L))
                .thenReturn(List.of(question));
        when(weaknessTagLogRepository.findByUser_UserIdOrderByTagLogIdDesc(7L)).thenReturn(List.of());

        var result = service.getUnusedGeneratedQuestionSets(7L).get(0);

        assertThat(result.getTargetWeaknessDisplayName()).isEqualTo("공고 요구사항 연결 부족");
        assertThat(result.getDisplayTitle()).isEqualTo("공고 요구사항 연결 부족");
    }

    private QuestionSet questionSet(Long id) {
        QuestionSet value = mock(QuestionSet.class);
        when(value.getQuestionSetId()).thenReturn(id);
        return value;
    }
}
