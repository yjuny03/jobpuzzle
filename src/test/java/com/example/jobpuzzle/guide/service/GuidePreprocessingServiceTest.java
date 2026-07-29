package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.client.GuideStructuringClient;
import com.example.jobpuzzle.guide.dto.GuideListResponse;
import com.example.jobpuzzle.guide.dto.GuidePreprocessRequest;
import com.example.jobpuzzle.guide.dto.GuidePreprocessingResult;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuidePreprocessingServiceTest {

    @Mock private GuideStructuringClient structuringClient;
    @Mock private GuidePreprocessingStateService stateService;
    @InjectMocks private GuidePreprocessingService service;

    private User admin;
    private GuidePreprocessRequest request;

    @BeforeEach
    void setUp() {
        admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        request = mock(GuidePreprocessRequest.class);
        when(request.getSourceText()).thenReturn("가이드 원문");
    }

    @Test
    void completesStructuredResultAfterMarkingProcessing() {
        GuidePreprocessingResult result = result();
        GuideListResponse response = mock(GuideListResponse.class);
        when(structuringClient.structure("가이드 원문")).thenReturn(result);
        when(structuringClient.model()).thenReturn("gpt-5.6-terra");
        when(stateService.complete(1L, "gpt-5.6-terra", result)).thenReturn(response);

        assertThat(service.preprocess(admin, 1L, request)).isSameAs(response);

        var order = inOrder(stateService, structuringClient);
        order.verify(stateService).begin(1L);
        order.verify(structuringClient).structure("가이드 원문");
        order.verify(stateService).complete(1L, "gpt-5.6-terra", result);
        verify(stateService, never()).fail(anyLong(), anyString());
    }

    @Test
    void recordsSafeFailureStateAndRethrowsProviderFailure() {
        CustomException failure = new CustomException(ErrorCode.GUIDE_PREPROCESSING_FAILED);
        when(structuringClient.structure("가이드 원문")).thenThrow(failure);

        assertThatThrownBy(() -> service.preprocess(admin, 1L, request))
                .isSameAs(failure);
        verify(stateService).begin(1L);
        verify(stateService).fail(1L, ErrorCode.GUIDE_PREPROCESSING_FAILED.getMessage());
        verify(stateService, never()).complete(anyLong(), anyString(), any());
    }

    private GuidePreprocessingResult result() {
        return new GuidePreprocessingResult(
                "백엔드 신입",
                List.of("문제 해결"),
                List.of("구체적인 근거 확인"),
                List.of("경험 질문"),
                List.of(),
                List.of(new GuidePreprocessingResult.Chunk("평가 기준", "내용", "요약"))
        );
    }
}
