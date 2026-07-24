package com.example.jobpuzzle.document.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "user_document_file")
public class UserDocumentFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private UserDocument document;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    // 자료 내 업로드 순서 (1부터 시작)
    @Column(name = "page_order", nullable = false)
    private int pageOrder;

    @Builder
    private UserDocumentFile(UserDocument document, String filePath, String fileName, int pageOrder) {
        this.document = document;
        this.filePath = filePath;
        this.fileName = fileName;
        this.pageOrder = pageOrder;
    }
}