package com.example.jobpuzzle.guide.controller;

import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.guide.dto.*;
import com.example.jobpuzzle.guide.service.GuidePreprocessingService;
import com.example.jobpuzzle.guide.service.GuideIndexingService;
import com.example.jobpuzzle.guide.service.GuideService;
import com.example.jobpuzzle.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/guide-admin")
public class GuideAdminController {

    private final GuideService guideService;
    private final GuidePreprocessingService guidePreprocessingService;
    private final GuideIndexingService guideIndexingService;

    /** 새 가이드 계보의 첫 초안을 등록한다. */
    @PostMapping("/guides")
    public ResponseEntity<ApiResponse<GuideListResponse>> registerGuide(
            @AuthenticationPrincipal(expression = "user") User admin,
            @Valid @RequestBody GuideRegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(guideService.registerGuideDocument(admin, request)));
    }

    /** 선택한 최신 가이드에서 다음 버전의 초안을 만든다. */
    @PostMapping("/guides/{guideId}/versions")
    public ResponseEntity<ApiResponse<GuideListResponse>> createVersion(
            @AuthenticationPrincipal(expression = "user") User admin,
            @PathVariable Long guideId,
            @Valid @RequestBody GuideVersionCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(guideService.createNextVersion(admin, guideId, request)));
    }

    /** 관리자 검수가 끝난 청크 목록으로 DRAFT의 내용을 전체 교체한다. */
    @PutMapping("/guides/{guideId}/chunks")
    public ResponseEntity<ApiResponse<GuideListResponse>> replaceChunks(
            @PathVariable Long guideId,
            @Valid @RequestBody GuideChunkReplaceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                guideService.replaceDraftChunks(guideId, request)));
    }

    /**
     * 최신 DRAFT 원문을 OpenAI로 구조화한다.
     * 응답은 검수 준비 상태까지만 저장하며 활성화는 별도 API로 관리자가 결정한다.
     */
    @PostMapping("/guides/{guideId}/preprocess")
    public ResponseEntity<ApiResponse<GuideListResponse>> preprocessGuide(
            @AuthenticationPrincipal(expression = "user") User admin,
            @PathVariable Long guideId,
            @Valid @RequestBody GuidePreprocessRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                guidePreprocessingService.preprocess(admin, guideId, request)));
    }

    /** 검수 준비가 끝난 최신 DRAFT 청크를 임베딩하고 벡터 저장소에 반영한다. */
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

    /** 관리 화면에서 단일 가이드의 버전·상태·청크 수를 조회한다. */
    @GetMapping("/guides/{guideId}")
    public ResponseEntity<ApiResponse<GuideListResponse>> getGuide(@PathVariable Long guideId) {
        return ResponseEntity.ok(ApiResponse.success(guideService.getGuide(guideId)));
    }
}
