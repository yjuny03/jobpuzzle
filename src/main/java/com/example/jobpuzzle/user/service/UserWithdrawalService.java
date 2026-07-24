package com.example.jobpuzzle.user.service;

import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseSourceRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisInputSnapshotSourceRepository;
import com.example.jobpuzzle.analysis.repository.CandidateMaterialAnalysisRepository;
import com.example.jobpuzzle.analysis.repository.ConfirmedAnalysisSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.JobPostingAnalysisRepository;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.repository.DocumentExtractionRepository;
import com.example.jobpuzzle.document.repository.UserDocumentRepository;
import com.example.jobpuzzle.document.storage.FileStorage;
import com.example.jobpuzzle.guide.repository.GuideContextChunkRepository;
import com.example.jobpuzzle.guide.repository.GuideContextResultRepository;
import com.example.jobpuzzle.analysis.repository.ActionPlanRepository;
import com.example.jobpuzzle.analysis.repository.MatchAnalysisResultRepository;
import com.example.jobpuzzle.analysis.repository.ReadinessResultRepository;
import com.example.jobpuzzle.interview.repository.InterviewQuestionRepository;
import com.example.jobpuzzle.interview.repository.QuestionSetRepository;
import com.example.jobpuzzle.jobposting.repository.JobPostingRepository;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원 탈퇴 시 여러 도메인에 흩어진 회원 소유 데이터를 FK 의존 순서(자식 -> 부모)대로 삭제한다.
// 면접/평가/추천 로그처럼 User를 FK로 참조하지 않는 테이블은 이번 범위에서 제외했다.
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final AnalysisCaseSourceRepository analysisCaseSourceRepository;
    private final AnalysisInputSnapshotSourceRepository analysisInputSnapshotSourceRepository;
    private final ConfirmedAnalysisSnapshotRepository confirmedAnalysisSnapshotRepository;
    private final AnalysisInputSnapshotRepository analysisInputSnapshotRepository;
    private final JobPostingAnalysisRepository jobPostingAnalysisRepository;
    private final CandidateMaterialAnalysisRepository candidateMaterialAnalysisRepository;
    private final ActionPlanRepository actionPlanRepository;
    private final ReadinessResultRepository readinessResultRepository;
    private final MatchAnalysisResultRepository matchAnalysisResultRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final QuestionSetRepository questionSetRepository;
    private final GuideContextChunkRepository guideContextChunkRepository;
    private final GuideContextResultRepository guideContextResultRepository;
    private final AnalysisCaseRepository analysisCaseRepository;
    private final JobPostingRepository jobPostingRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    @Transactional
    public void deleteAllDataAndUser(Long userId) {
        analysisCaseSourceRepository.deleteByAnalysisCase_User_UserId(userId);

        // 구형 확정 결과가 분석 결과를 참조하므로 결과 행보다 먼저 삭제한다.
        confirmedAnalysisSnapshotRepository.deleteAllRelatedToUser(userId);
        // JSON-05 자식 결과부터 제거해 snapshot과 AI 결과 FK 제약을 보존한다.
        interviewQuestionRepository.deleteByQuestionSet_Snapshot_User_UserId(userId);
        questionSetRepository.deleteBySnapshot_User_UserId(userId);
        actionPlanRepository.deleteBySnapshot_User_UserId(userId);
        readinessResultRepository.deleteBySnapshot_User_UserId(userId);
        matchAnalysisResultRepository.deleteBySnapshot_User_UserId(userId);
        jobPostingAnalysisRepository.deleteBySnapshot_User_UserId(userId);
        candidateMaterialAnalysisRepository.deleteBySnapshot_User_UserId(userId);
        guideContextChunkRepository.deleteByGuideContextResult_User_UserId(userId);
        guideContextResultRepository.deleteByUser_UserId(userId);

        // 분석 결과 삭제 후 snapshot의 자식부터 부모 순서로 제거한다.
        analysisInputSnapshotSourceRepository.deleteBySnapshot_User_UserId(userId);
        analysisInputSnapshotRepository.deleteByUser_UserId(userId);
        analysisCaseRepository.deleteByUser_UserId(userId);
        jobPostingRepository.deleteByUser_UserId(userId);

        documentExtractionRepository.clearBaseExtractionByUserId(userId);
        documentExtractionRepository.deleteByDocument_User_UserId(userId);

        for (UserDocument document : userDocumentRepository.findByUser_UserId(userId)) {
            if (document.getFilePath() != null) {
                fileStorage.delete(document.getFilePath());
            }
        }
        userDocumentRepository.deleteByUser_UserId(userId);

        userRepository.deleteById(userId);
    }
}
