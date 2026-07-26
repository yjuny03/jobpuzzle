package com.example.jobpuzzle.analysis.rag.service;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.rag.index.VectorIndexPort;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisMaterialChunkRepository;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor public class AnalysisVectorIndexService { private final AnalysisInputSnapshotRepository snapshots; private final AnalysisMaterialChunkRepository chunks; private final VectorIndexPort index;
 @Transactional(readOnly=true) public void index(Long userId,Long caseId){AnalysisInputSnapshot s=snapshot(userId,caseId); index.index(chunks.findByUserIdAndSnapshot_SnapshotIdOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(userId,s.getSnapshotId()));}
 @Transactional(readOnly=true) public void requireReady(Long userId,Long caseId){AnalysisInputSnapshot s=snapshot(userId,caseId); index.requireReady(userId,s.getSnapshotId(),chunks.findByUserIdAndSnapshot_SnapshotIdOrderBySnapshotSource_SnapshotSourceIdAscChunkIndexAsc(userId,s.getSnapshotId()));}
 private AnalysisInputSnapshot snapshot(Long userId,Long caseId){return snapshots.findByAnalysisCase_AnalysisCaseIdAndUser_UserId(caseId,userId).orElseThrow(()->new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));}}
