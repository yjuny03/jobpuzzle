package com.example.jobpuzzle.document.dto;

// 이미 확정 이력이 있는 자료를 다시 수정할 때 버전을 어느 단위로 올릴지 사용자가 선택
public enum ChangeType {
    MINOR, // 자잘한 수정 (예: 1.0 -> 1.1)
    MAJOR  // 큰 폭의 수정 (예: 1.1 -> 2.0)
}