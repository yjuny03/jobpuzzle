package com.example.jobpuzzle.analysis.rag.repository;

import com.example.jobpuzzle.analysis.rag.entity.RequirementRetrievalChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RequirementRetrievalChunkRepository extends JpaRepository<RequirementRetrievalChunk, Long> {

    // 고정 retrieval 결과를 저장 당시 순서대로 복원해 재사용 정합성을 검증한다.
    List<RequirementRetrievalChunk> findByRetrievalResult_RetrievalResultIdOrderByRankAsc(Long retrievalResultId);

    // result 삭제 전 하위 FK row를 먼저 제거해 unique 재생성을 가능하게 한다.
    void deleteByRetrievalResult_RetrievalResultId(Long retrievalResultId);

    // 여러 retrieval result를 정리할 때 하위 FK row를 한 번에 먼저 제거한다.
    void deleteByRetrievalResult_RetrievalResultIdIn(Collection<Long> retrievalResultIds);

    // 재생성 대상 source·version 청크를 참조하는 retrieval result만 좁혀 찾는다.
    @Query("select distinct chunk.retrievalResult.retrievalResultId from RequirementRetrievalChunk chunk "
            + "where chunk.materialChunk.snapshotSource.snapshotSourceId = :snapshotSourceId "
            + "and chunk.materialChunk.chunkingVersion = :chunkingVersion")
    List<Long> findDistinctRetrievalResultIdsByMaterialChunkSnapshotSourceIdAndChunkingVersion(
            @Param("snapshotSourceId") Long snapshotSourceId, @Param("chunkingVersion") String chunkingVersion);
}
