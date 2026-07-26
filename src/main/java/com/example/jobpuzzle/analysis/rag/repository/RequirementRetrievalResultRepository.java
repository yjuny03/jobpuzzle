package com.example.jobpuzzle.analysis.rag.repository;

import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalResult;
import com.example.jobpuzzle.analysis.rag.entity.RetrievalCorpusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RequirementRetrievalResultRepository extends JpaRepository<RequirementRetrievalResult, Long> {

    // snapshot requirement corpus의 기존 고정 검색 결과를 찾아 재사용 여부를 판단한다.
    Optional<RequirementRetrievalResult> findBySnapshot_SnapshotIdAndRequirementIdAndCorpusType(
            Long snapshotId, String requirementId, RetrievalCorpusType corpusType);

    // 사용자가 소유한 snapshot의 retrieval 전체를 안전하게 조회한다.
    List<RequirementRetrievalResult> findByUserIdAndSnapshot_SnapshotIdOrderByRetrievalResultIdAsc(Long userId, Long snapshotId);

    // snapshot 단위 정리·검증에 필요한 retrieval 전체를 조회한다.
    List<RequirementRetrievalResult> findBySnapshot_SnapshotIdOrderByRetrievalResultIdAsc(Long snapshotId);

    // 회원탈퇴 시 하위 retrieval chunk를 먼저 삭제할 result ID를 찾는다.
    @Query("select result.retrievalResultId from RequirementRetrievalResult result where result.userId = :userId")
    List<Long> findRetrievalResultIdsByUserId(@Param("userId") Long userId);

    // 하위 chunk 정리 뒤 회원 소유 retrieval result를 제거한다.
    void deleteByUserId(Long userId);

    // 재생성으로 무효화된 retrieval result만 제한해 제거한다.
    void deleteByRetrievalResultIdIn(Collection<Long> retrievalResultIds);
}
