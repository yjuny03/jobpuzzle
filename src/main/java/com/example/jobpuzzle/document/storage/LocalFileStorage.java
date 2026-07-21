package com.example.jobpuzzle.document.storage;

import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path baseDir;

    public LocalFileStorage(@Value("${app.storage.local.base-dir}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Override
    public String store(MultipartFile file, UserDocumentType documentType) {
        String storagePath = documentType.name().toLowerCase()
                + "/" + UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        Path targetPath = resolve(storagePath);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }

        return storagePath;
    }

    @Override
    public InputStream load(String storagePath) {
        try {
            return Files.newInputStream(resolve(storagePath));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(resolve(storagePath));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    // storagePath가 baseDir 밖을 가리키지 않도록 검증 후 실제 경로로 변환
    private Path resolve(String storagePath) {
        Path targetPath = baseDir.resolve(storagePath).normalize();
        if (!targetPath.startsWith(baseDir)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return targetPath;
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null) {
            return "";
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        return dotIndex >= 0 ? originalFileName.substring(dotIndex) : "";
    }
}