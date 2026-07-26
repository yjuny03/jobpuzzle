package com.example.jobpuzzle.document.dto;

import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 컨트롤러가 @RequestPart/@RequestParam으로 받은 값을 그대로 옮겨 담는 값 객체 (Spring 바인딩 대상 아님)
@Getter
@AllArgsConstructor
public class DocumentUploadRequest {

    private final UserDocumentType documentType;
    private final List<MultipartFile> files;
    private final String displayName;
    private final boolean keepOriginal;
}