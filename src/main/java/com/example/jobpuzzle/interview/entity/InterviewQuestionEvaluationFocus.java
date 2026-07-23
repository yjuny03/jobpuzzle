package com.example.jobpuzzle.interview.entity;

// JSON-05의 평가 관점 wire 값은 v13 lowerCamelCase 계약을 그대로 사용한다.
public enum InterviewQuestionEvaluationFocus {
    intentMatch,
    specificity,
    ownRole,
    problemSolving,
    resultExpression,
    requirementConnection,
    guideAlignment,
    deliveryClarity
}
