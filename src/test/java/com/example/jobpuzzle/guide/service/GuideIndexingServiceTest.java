package com.example.jobpuzzle.guide.service;

import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.dto.GuideListResponse;
import com.example.jobpuzzle.guide.vector.GuideVectorDocument;
import com.example.jobpuzzle.guide.vector.GuideVectorStorePort;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuideIndexingServiceTest {

    @Mock private GuideIndexingStateService stateService;
    @Mock private GuideVectorStorePort vectorStore;
    @InjectMocks private GuideIndexingService service;

    @Test
    void storesProviderContractOnlyAfterVectorIndexSucceeds() {
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        GuideVectorDocument document = new GuideVectorDocument(10L, 1L, "내용");
        when(stateService.begin(1L))
                .thenReturn(new GuideIndexingLease(1L, List.of(document)));
        when(vectorStore.index(1L, List.of(document)))
                .thenReturn(Map.of(10L, "guide-collection:10"));
        when(vectorStore.provider()).thenReturn("openai");
        when(vectorStore.model()).thenReturn("text-embedding-3-small");
        when(vectorStore.dimension()).thenReturn(1536);
        GuideListResponse response = mock(GuideListResponse.class);
        when(stateService.complete(
                1L, Map.of(10L, "guide-collection:10"),
                "openai", "text-embedding-3-small", 1536))
                .thenReturn(response);

        assertThat(service.index(admin, 1L)).isSameAs(response);
        verify(stateService, never()).fail(anyLong(), anyString());
        verify(vectorStore, times(1)).provider();
        verify(vectorStore, times(1)).model();
        verify(vectorStore, times(1)).dimension();
    }

    @Test
    void recordsFailureWhenVectorProviderMetadataCannotBeRead() {
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        GuideVectorDocument document = new GuideVectorDocument(10L, 1L, "내용");
        when(stateService.begin(1L))
                .thenReturn(new GuideIndexingLease(1L, List.of(document)));
        CustomException failure = new CustomException(ErrorCode.GUIDE_INDEXING_FAILED);
        when(vectorStore.provider()).thenThrow(failure);

        assertThatThrownBy(() -> service.index(admin, 1L)).isSameAs(failure);
        verify(stateService).fail(1L, ErrorCode.GUIDE_INDEXING_FAILED.getMessage());
        verify(vectorStore, never()).index(anyLong(), anyList());
    }
}
