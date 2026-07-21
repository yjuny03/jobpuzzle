package com.example.jobpuzzle.document.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExtractionEditRequest {

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    // 이 자료가 처음 확정되기 전(아직 버전이 없는 상태)이면 무시, 첫 확정 이후 수정부터는 필수
    private ChangeType changeType;
}