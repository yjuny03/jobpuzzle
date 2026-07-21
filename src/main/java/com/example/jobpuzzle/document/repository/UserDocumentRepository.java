package com.example.jobpuzzle.document.repository;

import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {

    Optional<UserDocument> findByDocumentIdAndUser_UserId(Long documentId, Long userId);

    Page<UserDocument> findByUser_UserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Page<UserDocument> findByUser_UserIdAndDocumentTypeAndDeletedAtIsNull(
            Long userId, UserDocumentType documentType, Pageable pageable
    );
}