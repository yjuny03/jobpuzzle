package com.example.jobpuzzle.document.repository;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, Long> {
    
    List<DocumentExtraction> findByDocument_DocumentIdOrderByExtractionIdDesc(Long documentId);

    Optional<DocumentExtraction> findTopByDocument_DocumentIdAndVersionStatusOrderByExtractionIdDesc(
            Long documentId, DocumentVersionStatus versionStatus
    );

    // 이 문서가 지금까지 한 번이라도 버전을 부여받은 적이 있는지, 있다면 가장 최근 버전이 뭔지 조회
    Optional<DocumentExtraction> findTopByDocument_DocumentIdAndMajorVersionIsNotNullOrderByExtractionIdDesc(
            Long documentId
    );

    Optional<DocumentExtraction> findByExtractionIdAndDocument_User_UserId(Long extractionId, Long userId);

    List<DocumentExtraction> findByExtractionIdInAndDocument_User_UserId(List<Long> extractionIds, Long userId);
}