package com.example.jobpuzzle.interview.dto;

import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InterviewModeAvailabilityResponse {
    private List<ModeAvailability> modes;

    @Getter
    @Builder
    public static class ModeAvailability {
        private InterviewSessionMode mode;
        private boolean available;
        private String reason;
    }
}
