package com.example.jobpuzzle.evaluation.dto;

import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SessionScoreSummary {
    private Long sessionId;
    private InterviewSessionMode mode;
    private Integer overallScore;
    private Map<String, Integer> categoryScores;
    private int totalQuestionCount;
    private int submittedQuestionCount;
    private int evaluatedQuestionCount;
    private int evaluationFailedQuestionCount;
    private int skippedQuestionCount;
    private int completionRate;
    private List<QuestionScore> questionScores;

    @Getter
    @Builder
    public static class QuestionScore {
        private Long sessionQuestionId;
        private Integer finalScore;
        private Map<String, Integer> dimensionScores;
        private Map<String, Integer> dimensionEvaluationCounts;
    }
}
