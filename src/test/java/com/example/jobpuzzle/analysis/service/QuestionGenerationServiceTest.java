package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.interview.entity.InterviewQuestion;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.QuestionSet;
import com.example.jobpuzzle.interview.entity.QuestionSetStatus;
import com.example.jobpuzzle.interview.repository.InterviewQuestionRepository;
import com.example.jobpuzzle.interview.repository.InterviewSessionRepository;
import com.example.jobpuzzle.interview.repository.QuestionSetRepository;
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

    private QuestionSet questionSet(Long id) {
        QuestionSet value = mock(QuestionSet.class);
        when(value.getQuestionSetId()).thenReturn(id);
        return value;
    }
}
