package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.AnalysisMaterialChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisMaterialChunkRepository extends JpaRepository<AnalysisMaterialChunk, Long> {

    // 같은 source·분할 규칙의 청크가 이미 생성됐는지 빠르게 판별한다.
    boolean existsBySnapshotSource_SnapshotSourceIdAndChunkingVersion(Long snapshotSourceId, String chunkingVersion);

    // source별 고정 순서로 기존 청크를 읽어 재사용·정합성 검증에 사용한다.
    List<AnalysisMaterialChunk> findBySnapshotSource_SnapshotSourceIdAndChunkingVersionOrderByChunkIndexAsc(
            Long snapshotSourceId, String chunkingVersion);

    // 불완전하거나 손상된 source·버전 청크 집합을 재생성 전에 제거한다.
    void deleteBySnapshotSource_SnapshotSourceIdAndChunkingVersion(Long snapshotSourceId, String chunkingVersion);

    // 회원탈퇴 시 snapshot source보다 먼저 material chunk FK row를 제거한다.
    void deleteBySnapshot_User_UserId(Long userId);

    // 사용자와 snapshot을 동시에 제한해 다른 사용자의 검색 후보가 섞이지 않게 조회한다.
    List<AnalysisMaterialChunk> findByUserIdAndSnapshot_SnapshotIdOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(
            Long userId, Long snapshotId);

    // candidate 유형과 청크 규칙 버전까지 DB에서 제한해 다른 corpus·버전 청크가 후보에 섞이지 않게 한다.
    List<AnalysisMaterialChunk> findByUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersionOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(
            Long userId, Long snapshotId, java.util.Collection<com.example.jobpuzzle.document.entity.UserDocumentType> documentTypes,
            String chunkingVersion);

    // 검색 결과 chunk ID를 소유 snapshot·candidate 유형 범위에서 다시 검증해 외부 adapter 오염을 차단한다.
    List<AnalysisMaterialChunk> findByChunkIdInAndUserIdAndSnapshot_SnapshotIdAndDocumentTypeInAndChunkingVersion(
            java.util.Collection<Long> chunkIds, Long userId, Long snapshotId,
            java.util.Collection<com.example.jobpuzzle.document.entity.UserDocumentType> documentTypes, String chunkingVersion);
}
