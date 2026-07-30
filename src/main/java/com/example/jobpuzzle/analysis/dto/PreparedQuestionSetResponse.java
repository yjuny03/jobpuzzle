package com.example.jobpuzzle.analysis.dto;

import com.example.jobpuzzle.interview.entity.QuestionSet;
import com.example.jobpuzzle.evaluation.entity.WeaknessTagLog;
import com.example.jobpuzzle.evaluation.service.WeaknessTagNormalizer;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PreparedQuestionSetResponse {
    private Long questionSetId;
    private String mode;
    private String targetWeaknessTag;
    private String targetWeaknessDisplayName;
    private String displayTitle;
    private String mainCategory;
    private String subCategory;
    private String careerLevel;
    private int questionCount;
    private LocalDateTime createdAt;
    private List<Occurrence> recentOccurrences;

    public static PreparedQuestionSetResponse from(
            QuestionSet set,
            int questionCount,
            WeaknessTagNormalizer normalizer,
            List<WeaknessTagLog> weaknessLogs
    ) {
        boolean weakness = set.getInterviewMode() == InterviewSessionMode.WEAKNESS_REVIEW;
        String weaknessName = weakness
                ? normalizer.displayName(set.getTargetWeaknessTag())
                : null;
        String mainCategory = set.getJobCategory() == null
                ? null : set.getJobCategory().getMainCategory();
        String subCategory = set.getJobCategory() == null
                ? null : set.getJobCategory().getSubCategory();
        String careerLevel = careerLevelLabel(set.getCareerLevel());
        String basicTitle = String.join(" · ", java.util.stream.Stream
                .of(mainCategory, subCategory, careerLevel)
                .filter(value -> value != null && !value.isBlank())
                .toList());
        return PreparedQuestionSetResponse.builder()
                .questionSetId(set.getQuestionSetId())
                .mode(set.getInterviewMode().name())
                .targetWeaknessTag(set.getTargetWeaknessTag())
                .targetWeaknessDisplayName(weaknessName)
                .displayTitle(weakness ? weaknessName : basicTitle)
                .mainCategory(mainCategory)
                .subCategory(subCategory)
                .careerLevel(careerLevel)
                .questionCount(questionCount)
                .createdAt(set.getCreatedAt())
                .recentOccurrences((weaknessLogs == null ? List.<WeaknessTagLog>of() : weaknessLogs)
                        .stream()
                        .limit(5)
                        .map(log -> Occurrence.builder()
                                .occurredAt(log.getCreatedAt())
                                .mode(modeLabel(log.getSession() == null
                                        ? null : log.getSession().getMode()))
                                .score(log.getEvaluation() == null
                                        ? null : log.getEvaluation().getScore())
                                .build())
                        .toList())
                .build();
    }

    private static String careerLevelLabel(JobCategoryCareerLevel careerLevel) {
        if (careerLevel == null) return null;
        return switch (careerLevel) {
            case NEW -> "신입";
            case EXPERIENCED -> "경력";
            case ANY -> "경력 무관";
        };
    }

    private static String modeLabel(InterviewSessionMode mode) {
        if (mode == null) return null;
        return switch (mode) {
            case BASIC -> "기본 질문";
            case COMPANY_FIT -> "회사 맞춤";
            case WEAKNESS_REVIEW -> "약점 보완";
        };
    }

    @Getter
    @Builder
    public static class Occurrence {
        private LocalDateTime occurredAt;
        private String mode;
        private Integer score;
    }
}
