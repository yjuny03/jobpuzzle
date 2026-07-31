package com.example.jobpuzzle.evaluation.service;

import com.example.jobpuzzle.interview.entity.InterviewQuestionEvaluationFocus;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * AI가 생성한 세부 진단 키워드와 과거 태그를 8개 평가 관점으로 정규화한다.
 */
@Component
public class WeaknessTagNormalizer {

    public String canonicalTag(String tag) {
        return dimension(tag) + "_weak";
    }

    public String dimension(String tag) {
        String value = normalized(tag);

        if (containsAny(value, "requirementconnection", "requirementlink",
                "요구사항연결", "공고연결", "회사연결")) {
            return InterviewQuestionEvaluationFocus.requirementConnection.name();
        }
        if (containsAny(value, "guidealignment", "technicaldepth",
                "insufficientcloudexperience", "cloudexperience",
                "직무기준", "직무가이드", "기술깊이", "실무경험", "기술스택")) {
            return InterviewQuestionEvaluationFocus.guideAlignment.name();
        }
        if (containsAny(value, "ownrole", "vaguerole", "limitedownership",
                "ownership", "본인역할", "역할불명확", "역할설명", "주도성")) {
            return InterviewQuestionEvaluationFocus.ownRole.name();
        }
        if (containsAny(value, "problemsolving", "implementationprocess",
                "문제해결", "구현과정", "해결과정")) {
            return InterviewQuestionEvaluationFocus.problemSolving.name();
        }
        if (containsAny(value, "resultexpression", "quantification",
                "성과근거", "결과정량", "결과표현", "성과표현")) {
            return InterviewQuestionEvaluationFocus.resultExpression.name();
        }
        if (containsAny(value, "deliveryclarity", "clarity",
                "답변전달", "전달력", "표현명확")) {
            return InterviewQuestionEvaluationFocus.deliveryClarity.name();
        }
        if (containsAny(value, "specificity", "lackofspecificdetail",
                "구체성", "세부구성", "사례부족", "행동설명")) {
            return InterviewQuestionEvaluationFocus.specificity.name();
        }
        return InterviewQuestionEvaluationFocus.intentMatch.name();
    }

    public String displayName(String tag) {
        return switch (dimension(tag)) {
            case "requirementConnection" -> "공고 요구사항 연결 부족";
            case "specificity" -> "답변의 구체성 부족";
            case "ownRole" -> "본인 역할 설명 부족";
            case "problemSolving" -> "문제 해결 과정 부족";
            case "resultExpression" -> "성과 및 결과 표현 부족";
            case "guideAlignment" -> "직무 기준 연결 부족";
            case "deliveryClarity" -> "답변 전달력 부족";
            default -> "질문 의도 파악 부족";
        };
    }

    public boolean sameDimension(String left, String right) {
        return dimension(left).equals(dimension(right));
    }

    public String diagnosticDisplayName(String tag) {
        String value = normalized(tag);
        if (containsAny(value, "lackofspecificdetail", "specificity", "구체성")) {
            return "구체성 부족";
        }
        if (containsAny(value, "insufficientcloudexperience", "cloudexperience", "클라우드")) {
            return "클라우드 경험 부족";
        }
        if (containsAny(value, "lackoftechnicaldepth", "technicaldepth", "기술깊이")) {
            return "기술적 깊이 부족";
        }
        if (containsAny(value, "vaguerole", "ownrole", "역할불명확", "역할설명")) {
            return "역할 설명 부족";
        }
        if (containsAny(value, "limitedownership", "ownership", "주도성")) {
            return "주도성 부족";
        }
        if (containsAny(value, "resultexpression", "성과근거", "결과정량", "결과표현")) {
            return "성과 근거 부족";
        }
        if (containsAny(value, "requirementconnection", "요구사항연결", "공고연결")) {
            return "요구사항 연결 부족";
        }
        if (containsAny(value, "guidealignment", "직무기준", "직무가이드")) {
            return "직무 기준 연결 부족";
        }
        if (containsAny(value, "deliveryclarity", "답변전달", "표현명확")) {
            return "답변 전달력 부족";
        }
        if (containsAny(value, "problemsolving", "구현과정", "문제해결")) {
            return "구현 과정 설명 부족";
        }
        if (containsAny(value, "intentmatch", "질문의도")) {
            return "질문 의도 파악 부족";
        }
        String original = tag == null ? "" : tag.replace("#", "").trim();
        return original.isBlank() ? displayName(tag) : original;
    }

    private String normalized(String tag) {
        return (tag == null ? "" : tag.replace("#", "").trim())
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate.toLowerCase(Locale.ROOT)
                    .replace("_", "").replace("-", "").replace(" ", ""))) {
                return true;
            }
        }
        return false;
    }
}
