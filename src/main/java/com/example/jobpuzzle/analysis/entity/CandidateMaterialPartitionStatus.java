package com.example.jobpuzzle.analysis.entity;

// partition별 Provider 호출 상태다. 성공 payload는 SUCCEEDED일 때만 재사용할 수 있다.
public enum CandidateMaterialPartitionStatus {
    PENDING,
    RUNNING,
    FAILED,
    SPLIT,
    SUCCEEDED
}
