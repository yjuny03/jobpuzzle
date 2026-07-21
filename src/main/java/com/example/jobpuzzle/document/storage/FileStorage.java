package com.example.jobpuzzle.document.storage;

import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorage {

    //파일 저장 및 UserDocument.filePath에 기록할 저장소 내 상대 경로 반환
    String store(MultipartFile file, UserDocumentType documentType);

    InputStream load(String storagePath);

    void delete(String storagePath);
}