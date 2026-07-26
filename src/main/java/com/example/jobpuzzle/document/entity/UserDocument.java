package com.example.jobpuzzle.document.entity;

import com.example.jobpuzzle.global.common.BaseEntity;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "user_document")
public class UserDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private UserDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private UserDocumentSourceType sourceType;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("pageOrder ASC")
    private List<UserDocumentFile> files = new ArrayList<>();

    @Column(name = "keep_original", nullable = false)
    private boolean keepOriginal = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private UserDocument(
            User user,
            UserDocumentType documentType,
            UserDocumentSourceType sourceType,
            String displayName,
            boolean keepOriginal
    ) {
        this.user = user;
        this.documentType = documentType;
        this.sourceType = sourceType;
        this.displayName = displayName;
        this.keepOriginal = keepOriginal;
    }

    public void updateKeepOriginal(boolean keepOriginal) {
        this.keepOriginal = keepOriginal;
    }

    // 업로드 순서대로 반복 호출되는 것을 전제로 pageOrder를 자동 채번
    public void addFile(String filePath, String fileName) {
        files.add(UserDocumentFile.builder()
                .document(this)
                .filePath(filePath)
                .fileName(fileName)
                .pageOrder(files.size() + 1)
                .build());
    }

    public void clearFiles() {
        files.clear();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}