package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.client.GuideStructuringClient;
import com.example.jobpuzzle.guide.dto.GuideListResponse;
import com.example.jobpuzzle.guide.dto.GuidePreprocessRequest;
import com.example.jobpuzzle.guide.dto.GuidePreprocessingResult;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * 짧은 상태 트랜잭션 사이에서 외부 OpenAI 호출을 수행하는 오케스트레이터.
 * 실패해도 가이드 버전과 기존 분석 스냅샷은 변경하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class GuidePreprocessingService {

    private final GuideStructuringClient structuringClient;
    private final GuidePreprocessingStateService stateService;

    public GuideListResponse preprocess(
            User admin, Long guideId, GuidePreprocessRequest request
    ) {
        requireAdmin(admin);
        stateService.begin(guideId);
        try {
            GuidePreprocessingResult result =
                    structuringClient.structure(request.getSourceText());
            return stateService.complete(guideId, structuringClient.model(), result);
        } catch (RuntimeException exception) {
            stateService.fail(guideId, safeFailureMessage(exception));
            throw exception;
        }
    }

    private String safeFailureMessage(RuntimeException exception) {
        if (exception instanceof CustomException customException) {
            return customException.getErrorCode().getMessage();
        }
        return ErrorCode.GUIDE_PREPROCESSING_FAILED.getMessage();
    }

    private void requireAdmin(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("관리자만 가이드를 전처리할 수 있습니다.");
        }
    }
}
