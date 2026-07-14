package com.example.jobpuzzle.document.repository;

import com.example.jobpuzzle.document.entity.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {
}
