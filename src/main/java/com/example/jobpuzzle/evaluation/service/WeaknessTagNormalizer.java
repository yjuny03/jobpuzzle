package com.example.jobpuzzle.evaluation.service;

import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 과거 한글 태그와 _weak/_insufficient 별칭을 8개 평가 관점으로 통합한다.
 */
@Component
public class WeaknessTagNormalizer {

    public String canonicalTag(String tag) {
        return dimension(tag) + "_weak";
    }

    public String dimension(String tag) {
        String value = tag == null ? "" : tag.replace("#", "").trim();
        String lower = value.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "requirementconnection", "요구사항", "공고 연결")) {
            return InterviewQuestionEvaluationFocus.requirementConnection.name();
        }
        if (containsAny(lower, "specificity", "구체")) {
            return InterviewQuestionEvaluationFocus.specificity.name();
        }
        if (containsAny(lower, "ownrole", "본인 역할", "역할 설명")) {
            return InterviewQuestionEvaluationFocus.ownRole.name();
        }
        if (containsAny(lower, "problemsolving", "문제 해결")) {
            return InterviewQuestionEvaluationFocus.problemSolving.name();
        }
        if (containsAny(lower, "resultexpression", "성과", "결과 표현")) {
            return InterviewQuestionEvaluationFocus.resultExpression.name();
        }
        if (containsAny(lower, "guidealignment", "기술스택", "기술 스택", "직무 가이드", "관련 경험")) {
            return InterviewQuestionEvaluationFocus.guideAlignment.name();
        }
        if (containsAny(lower, "deliveryclarity", "전달", "표현 명확")) {
            return InterviewQuestionEvaluationFocus.deliveryClarity.name();
        }
        return InterviewQuestionEvaluationFocus.intentMatch.name();
    }

    public String displayName(String tag) {
        return switch (dimension(tag)) {
            case "requirementConnection" -> "공고 요구사항 연결 부족";
            case "specificity" -> "답변의 구체성 부족";
            case "ownRole" -> "본인 역할 설명 부족";
            case "problemSolving" -> "문제 해결 과정 부족";
            case "resultExpression" -> "성과·결과 표현 부족";
            case "guideAlignment" -> "직무 기준 연결 부족";
            case "deliveryClarity" -> "답변 전달력 부족";
            default -> "질문 의도 파악 부족";
        };
    }

    public boolean sameDimension(String left, String right) {
        return dimension(left).equals(dimension(right));
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
