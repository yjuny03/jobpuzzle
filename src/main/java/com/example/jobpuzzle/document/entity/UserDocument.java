package com.example.jobpuzzle.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "user_document")
public class UserDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    private Long userId;

    private String documentType;

    private String sourceType;

    private String filePath;

    private String fileName;

    private Boolean keepOriginal;

    private Integer version;

    private Boolean isLatest;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

}
