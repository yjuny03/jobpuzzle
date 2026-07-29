package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.GuideListResponse;
import com.example.jobpuzzle.guide.vector.GuideVectorStorePort;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;

/** DB 트랜잭션 밖에서 임베딩과 벡터 저장을 실행하는 관리자 인덱싱 오케스트레이터다. */
@Service
@RequiredArgsConstructor
public class GuideIndexingService {

    private final GuideIndexingStateService stateService;
    private final GuideVectorStorePort vectorStore;

    public GuideListResponse index(User admin, Long guideId) {
        requireAdmin(admin);
        GuideIndexingLease lease = stateService.begin(guideId);
        try {
            Map<Long, String> references =
                    vectorStore.index(guideId, lease.documents());
            return stateService.complete(
                    guideId,
                    references,
                    vectorStore.provider(),
                    vectorStore.model(),
                    vectorStore.dimension());
        } catch (RuntimeException exception) {
            stateService.fail(guideId, safeFailureMessage(exception));
            throw exception;
        }
    }

    private String safeFailureMessage(RuntimeException exception) {
        if (exception instanceof CustomException customException) {
            return customException.getErrorCode().getMessage();
        }
        return ErrorCode.GUIDE_INDEXING_FAILED.getMessage();
    }

    private void requireAdmin(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("관리자만 가이드를 인덱싱할 수 있습니다.");
        }
    }
}
