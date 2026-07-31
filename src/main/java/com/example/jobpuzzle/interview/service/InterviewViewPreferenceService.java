package com.example.jobpuzzle.interview.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.interview.entity.InterviewSectionKey;
import com.example.jobpuzzle.interview.entity.InterviewViewPreference;
import com.example.jobpuzzle.interview.entity.InterviewViewPreferenceDefaults;
import com.example.jobpuzzle.interview.repository.InterviewViewPreferenceRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewViewPreferenceService {

    private final InterviewViewPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<InterviewSectionKey> getSectionOrder(Long userId) {
        return preferenceRepository.findByUser_UserId(userId)
                .map(InterviewViewPreference::getSectionOrder)
                .map(List::copyOf)
                .orElse(InterviewViewPreferenceDefaults.SECTION_ORDER);
    }

    public List<InterviewSectionKey> saveSectionOrder(
            Long userId,
            List<InterviewSectionKey> sectionOrder
    ) {
        validate(sectionOrder);
        InterviewViewPreference preference = preferenceRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                    return InterviewViewPreference.create(user);
                });
        preference.updateSectionOrder(sectionOrder);
        preferenceRepository.save(preference);
        return List.copyOf(preference.getSectionOrder());
    }

    private void validate(List<InterviewSectionKey> sectionOrder) {
        if (sectionOrder == null
                || sectionOrder.size() != InterviewViewPreferenceDefaults.SECTION_ORDER.size()
                || new HashSet<>(sectionOrder).size() != InterviewViewPreferenceDefaults.SECTION_ORDER.size()
                || !new HashSet<>(sectionOrder).containsAll(InterviewViewPreferenceDefaults.SECTION_ORDER)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
