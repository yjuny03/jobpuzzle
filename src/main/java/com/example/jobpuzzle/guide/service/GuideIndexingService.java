package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.GuideListResponse;
import com.example.jobpuzzle.guide.vector.GuideVectorStorePort;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;

/** DB 트랜잭션 밖에서 임베딩과 벡터 저장을 실행하는 관리자 인덱싱 오케스트레이터다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuideIndexingService {

    private final GuideIndexingStateService stateService;
    private final GuideVectorStorePort vectorStore;

    /** 관리자 요청 한 번으로 청크 임베딩, 벡터 저장, DB 참조 확정을 순서대로 수행한다. */
    public GuideListResponse index(User admin, Long guideId) {
        requireAdmin(admin);
        GuideIndexingLease lease = stateService.begin(guideId);
        try {
            String provider = vectorStore.provider();
            String model = vectorStore.model();
            int dimension = vectorStore.dimension();
            log.info("가이드 벡터 색인 시작 guideId={} documentCount={} provider={} model={}",
                    guideId, lease.documents().size(), provider, model);
            Map<Long, String> references =
                    vectorStore.index(guideId, lease.documents());
            GuideListResponse response = stateService.complete(
                    guideId,
                    references,
                    provider,
                    model,
                    dimension);
            log.info("가이드 벡터 색인 완료 guideId={} chunkCount={} dimension={}",
                    guideId, response.getChunkCount(), response.getEmbeddingDimension());
            return response;
        } catch (RuntimeException exception) {
            stateService.fail(guideId, safeFailureMessage(exception));
            log.error("가이드 벡터 색인 실패 guideId={} exceptionType={} errorCode={}",
                    guideId,
                    exception.getClass().getSimpleName(),
                    exception instanceof CustomException customException
                            ? customException.getErrorCode().name()
                            : ErrorCode.GUIDE_INDEXING_FAILED.name(),
                    exception);
            throw exception;
        }
    }

    /** 외부 Provider 상세 대신 관리자에게 노출 가능한 고정 실패 사유만 반환한다. */
    private String safeFailureMessage(RuntimeException exception) {
        if (exception instanceof CustomException customException) {
            return customException.getErrorCode().getMessage();
        }
        return ErrorCode.GUIDE_INDEXING_FAILED.getMessage();
    }

    /** 컨트롤러 보안과 별개로 서비스 진입점에서도 관리자 권한을 확인한다. */
    private void requireAdmin(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("관리자만 가이드를 인덱싱할 수 있습니다.");
        }
    }
}
