package com.example.jobpuzzle.interview.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.interview.entity.InterviewSectionKey;
import com.example.jobpuzzle.interview.entity.InterviewViewPreference;
import com.example.jobpuzzle.interview.repository.InterviewViewPreferenceRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewViewPreferenceServiceTest {

    @Mock private InterviewViewPreferenceRepository preferenceRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private InterviewViewPreferenceService service;

    @Test
    void returnsDefaultOrderWhenUserHasNoSavedPreference() {
        when(preferenceRepository.findByUser_UserId(7L)).thenReturn(Optional.empty());

        assertThat(service.getSectionOrder(7L)).containsExactly(
                InterviewSectionKey.ACTIVE_ANALYSIS,
                InterviewSectionKey.ACTIVE_INTERVIEW,
                InterviewSectionKey.PREPARED_QUESTION,
                InterviewSectionKey.REVIEW_INTERVIEW
        );
    }

    @Test
    void savesACompleteUniqueOrderForTheCurrentUser() {
        User user = mock(User.class);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUser_UserId(7L)).thenReturn(Optional.empty());
        List<InterviewSectionKey> requested = List.of(
                InterviewSectionKey.REVIEW_INTERVIEW,
                InterviewSectionKey.PREPARED_QUESTION,
                InterviewSectionKey.ACTIVE_INTERVIEW,
                InterviewSectionKey.ACTIVE_ANALYSIS
        );

        assertThat(service.saveSectionOrder(7L, requested)).containsExactlyElementsOf(requested);

        verify(preferenceRepository).save(org.mockito.ArgumentMatchers.argThat(value ->
                value.getUser() == user && value.getSectionOrder().equals(requested)
        ));
    }

    @Test
    void rejectsAnOrderWithMissingOrDuplicatedSections() {
        List<InterviewSectionKey> invalid = List.of(
                InterviewSectionKey.ACTIVE_ANALYSIS,
                InterviewSectionKey.ACTIVE_INTERVIEW,
                InterviewSectionKey.ACTIVE_INTERVIEW,
                InterviewSectionKey.REVIEW_INTERVIEW
        );

        assertThatThrownBy(() -> service.saveSectionOrder(7L, invalid))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void updatesTheExistingPreferenceInsteadOfCreatingAnotherRow() {
        User user = mock(User.class);
        InterviewViewPreference preference = InterviewViewPreference.create(user);
        when(preferenceRepository.findByUser_UserId(7L)).thenReturn(Optional.of(preference));
        List<InterviewSectionKey> requested = List.of(
                InterviewSectionKey.ACTIVE_INTERVIEW,
                InterviewSectionKey.ACTIVE_ANALYSIS,
                InterviewSectionKey.REVIEW_INTERVIEW,
                InterviewSectionKey.PREPARED_QUESTION
        );

        service.saveSectionOrder(7L, requested);

        assertThat(preference.getSectionOrder()).containsExactlyElementsOf(requested);
        verify(preferenceRepository).save(preference);
    }
}
