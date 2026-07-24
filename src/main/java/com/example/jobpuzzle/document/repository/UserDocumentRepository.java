package com.example.jobpuzzle.document.repository;

import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {

    Optional<UserDocument> findByDocumentIdAndUser_UserId(Long documentId, Long userId);

    // 비동기 추출 러너는 트랜잭션 없이 동작하므로, files(LAZY)를 나중에 접근할 수 있도록 미리 함께 가져옴
    @Query("SELECT d FROM UserDocument d LEFT JOIN FETCH d.files WHERE d.documentId = :documentId")
    Optional<UserDocument> findWithFilesById(@Param("documentId") Long documentId);

    Page<UserDocument> findByUser_UserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Page<UserDocument> findByUser_UserIdAndDocumentTypeAndDeletedAtIsNull(
            Long userId, UserDocumentType documentType, Pageable pageable
    );

    List<UserDocument> findByUser_UserId(Long userId);

    void deleteByUser_UserId(Long userId);
}