package com.example.jobpuzzle.document.repository;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, Long> {

    List<DocumentExtraction> findByDocument_DocumentIdOrderByVersionDesc(Long documentId);

    Optional<DocumentExtraction> findTopByDocument_DocumentIdAndVersionStatusOrderByVersionDesc(
            Long documentId, DocumentVersionStatus versionStatus
    );

    Optional<DocumentExtraction> findByExtractionIdAndDocument_User_UserId(Long extractionId, Long userId);

    List<DocumentExtraction> findByExtractionIdInAndDocument_User_UserId(List<Long> extractionIds, Long userId);

    @Query("SELECT MAX(e.version) FROM DocumentExtraction e WHERE e.document.documentId = :documentId")
    Optional<Integer> findMaxVersionByDocumentId(@Param("documentId") Long documentId);
}