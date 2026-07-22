package com.example.jobpuzzle.analysis.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// jobCategoryId를 안 보내면 회원 기본 관심 직무를 초기값으로 사용
@Getter
@NoArgsConstructor
public class AnalysisCaseCreateRequest {
    private Long jobCategoryId;
}