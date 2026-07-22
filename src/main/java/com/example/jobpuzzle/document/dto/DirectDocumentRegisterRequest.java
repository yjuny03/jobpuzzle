package com.example.jobpuzzle.document.dto;

import com.example.jobpuzzle.document.entity.UserDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 직접 입력 등록 - 채용공고/회사정보/경험정리만 허용 (DocumentService.registerTextDocument 참고)
@Getter
@NoArgsConstructor
public class DirectDocumentRegisterRequest {

    @NotNull(message = "자료 유형을 선택해주세요.")
    private UserDocumentType documentType;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    @NotBlank(message = "자료명을 입력해주세요.")
    private String displayName;
}