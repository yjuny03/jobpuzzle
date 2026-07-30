package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.interview.entity.FollowUpQuestionType;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerEvaluationResult {

    // 평가 대상 면접 모드
    private InterviewSessionMode interviewMode;

    // 현재 평가 중인 답변의 원 질문 기준 꼬리질문 깊이(0/1/2)
    private int currentFollowUpDepth;

    private int score;

    private String scoreLabel;

    private int passThreshold;

    private EvaluationDetail evaluationDetail;

    private List<String> weaknessTags;

    private String summary;

    private List<String> improvementDirection;

    private AnswerDisposition answerDisposition;

    // 다음 꼬리질문 1개 또는 null
    private FollowUp followUp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationDetail {
        private DimensionScore intentMatch;
        private DimensionScore specificity;
        private DimensionScore ownRole;
        private DimensionScore problemSolving;
        private DimensionScore resultExpression;
        // BASIC이면 score=null
        private DimensionScore requirementConnection;
        // 적용 가이드가 없으면 score=null
        private DimensionScore guideAlignment;
        private DimensionScore deliveryClarity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DimensionScore {
        private Integer score;
        private String comment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FollowUp {
        // currentFollowUpDepth+1과 같아야 하며 currentFollowUpDepth=2이면 followUp은 null
        private int depth;
        private String question;
        private FollowUpQuestionType type;
        // COMPANY_FIT 약점 태그. 특정 태그와 무관하거나 BASIC이면 null
        private String targetWeakness;
        private String reason;
    }
}
