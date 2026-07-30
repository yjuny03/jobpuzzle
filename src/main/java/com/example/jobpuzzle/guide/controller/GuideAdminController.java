package com.example.jobpuzzle.guide.controller;

import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.guide.dto.*;
import com.example.jobpuzzle.guide.service.GuideIndexingService;
import com.example.jobpuzzle.guide.service.GuideService;
import com.example.jobpuzzle.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guide-admin")
public class GuideAdminController {

    private final GuideService guideService;
    private final GuideIndexingService guideIndexingService;

    /** 관리자 검수가 끝난 청크 목록으로 DRAFT의 내용을 전체 교체한다. */
    @PutMapping("/guides/{guideId}/chunks")
    public ResponseEntity<ApiResponse<GuideListResponse>> replaceChunks(
            @PathVariable Long guideId,
            @Valid @RequestBody GuideChunkReplaceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                guideService.replaceDraftChunks(guideId, request)));
    }

    /** 등록 시 분할한 원문 청크를 순서대로 조회해 관리자 검수 화면에 제공한다. */
    @GetMapping("/guides/{guideId}/chunks")
    public ResponseEntity<ApiResponse<java.util.List<GuideChunkResponse>>> getChunks(
            @PathVariable Long guideId
    ) {
        return ResponseEntity.ok(ApiResponse.success(guideService.getChunks(guideId)));
    }

    /** 최신 DRAFT의 원문 청크를 임베딩하고 벡터 저장소에 반영한다. */
    @PostMapping("/guides/{guideId}/index")
    public ResponseEntity<ApiResponse<GuideListResponse>> indexGuide(
            @AuthenticationPrincipal(expression = "user") User admin,
            @PathVariable Long guideId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                guideIndexingService.index(admin, guideId)));
    }

    /** 최신 DRAFT를 활성화하고 같은 범위의 이전 ACTIVE를 비활성화한다. */
    @PostMapping("/guides/{guideId}/activate")
    public ResponseEntity<ApiResponse<GuideListResponse>> activateGuide(
            @AuthenticationPrincipal(expression = "user") User admin,
            @PathVariable Long guideId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                guideService.activateLatestVersion(admin, guideId)));
    }

}
