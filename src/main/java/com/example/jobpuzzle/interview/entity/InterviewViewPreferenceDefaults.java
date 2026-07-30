package com.example.jobpuzzle.interview.entity;

import java.util.List;

public final class InterviewViewPreferenceDefaults {

    public static final List<InterviewSectionKey> SECTION_ORDER = List.of(
            InterviewSectionKey.ACTIVE_ANALYSIS,
            InterviewSectionKey.ACTIVE_INTERVIEW,
            InterviewSectionKey.PREPARED_QUESTION,
            InterviewSectionKey.REVIEW_INTERVIEW
    );

    private InterviewViewPreferenceDefaults() {
    }
}
