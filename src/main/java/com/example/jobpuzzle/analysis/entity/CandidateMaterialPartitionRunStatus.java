package com.example.jobpuzzle.analysis.entity;

// JSON-02 분할 분석 batch의 내부 진행 상태다. AnalysisCase 상태와 분리해 재실행 대상을 판단한다.
public enum CandidateMaterialPartitionRunStatus {
    PENDING,
    RUNNING,
    FAILED,
    SUCCEEDED
}
