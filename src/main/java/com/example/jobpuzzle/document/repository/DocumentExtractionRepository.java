package com.example.jobpuzzle.document.repository;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // base_extraction_id가 같은 테이블의 다른 행을 가리키므로, 회원 탈퇴로 일괄 삭제하기 전에 먼저 끊어둠
    @Modifying(clearAutomatically = true)
    @Query("UPDATE DocumentExtraction e SET e.baseExtraction = null WHERE e.document.user.userId = :userId")
    void clearBaseExtractionByUserId(@Param("userId") Long userId);

    void deleteByDocument_User_UserId(Long userId);
}