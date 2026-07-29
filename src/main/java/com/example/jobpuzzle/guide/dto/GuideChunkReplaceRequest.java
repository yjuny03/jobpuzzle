package com.example.jobpuzzle.guide.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** DRAFT 가이드의 청크 전체를 한 번에 교체해 부분 저장으로 인한 순서 불일치를 막는다. */
@Getter
@NoArgsConstructor
public class GuideChunkReplaceRequest {
    @Valid
    @NotEmpty
    private List<GuideChunkRequest> chunks;
}
