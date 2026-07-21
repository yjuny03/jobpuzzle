package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.analysis.dto.AnalysisCaseCreateRequest;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseJobCategoryUpdateRequest;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseResponse;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseSourceAddRequest;
import com.example.jobpuzzle.analysis.dto.AnalysisCaseSourceResponse;
import com.example.jobpuzzle.analysis.entity.AnalysisCase;
import com.example.jobpuzzle.analysis.entity.AnalysisCaseSource;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseRepository;
import com.example.jobpuzzle.analysis.repository.AnalysisCaseSourceRepository;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.document.service.DocumentExtractionService;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisCaseService {

    private final AnalysisCaseRepository analysisCaseRepository;
    private final AnalysisCaseSourceRepository analysisCaseSourceRepository;
    private final UserRepository userRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final DocumentExtractionService documentExtractionService;

    // jobCategoryId를 안 보내면 회원 기본 관심 직무를 초기값으로 사용
    @Transactional
    public AnalysisCaseResponse createCase(Long userId, AnalysisCaseCreateRequest request) {
        JobCategory jobCategory = resolveInitialJobCategory(userId, request.getJobCategoryId());

        AnalysisCase analysisCase = AnalysisCase.builder()
                .user(userRepository.getReferenceById(userId))
                .jobCategory(jobCategory)
                .build();
        analysisCaseRepository.save(analysisCase);

        return AnalysisCaseResponse.of(analysisCase, List.of());
    }

    public AnalysisCaseResponse getCase(Long userId, Long analysisCaseId) {
        AnalysisCase analysisCase = findOwnedCase(userId, analysisCaseId);
        return AnalysisCaseResponse.of(analysisCase, findSources(analysisCaseId));
    }

    @Transactional
    public AnalysisCaseResponse changeJobCategory(Long userId, Long analysisCaseId, AnalysisCaseJobCategoryUpdateRequest request) {
        AnalysisCase analysisCase = findOwnedCase(userId, analysisCaseId);
        requireDraft(analysisCase);

        analysisCase.changeJobCategory(findJobCategory(request.getJobCategoryId()));

        return AnalysisCaseResponse.of(analysisCase, findSources(analysisCaseId));
    }

    // 본인 소유 CONFIRMED 추출본만 연결 가능. 채용공고는 1건만, 동일 추출본 중복 연결은 금지
    @Transactional
    public AnalysisCaseSourceResponse addSource(Long userId, Long analysisCaseId, AnalysisCaseSourceAddRequest request) {
        AnalysisCase analysisCase = findOwnedCase(userId, analysisCaseId);
        requireDraft(analysisCase);

        DocumentExtraction extraction =
                documentExtractionService.getConfirmedExtractions(userId, List.of(request.getExtractionId())).get(0);
        UserDocumentType documentType = extraction.getDocument().getDocumentType();

        if (analysisCaseSourceRepository.existsByAnalysisCase_AnalysisCaseIdAndExtraction_ExtractionId(
                analysisCaseId, extraction.getExtractionId())) {
            throw new CustomException(ErrorCode.ANALYSIS_CASE_SOURCE_DUPLICATE);
        }
        if (documentType == UserDocumentType.JOB_POSTING && analysisCaseSourceRepository
                .existsByAnalysisCase_AnalysisCaseIdAndDocumentType(analysisCaseId, UserDocumentType.JOB_POSTING)) {
            throw new CustomException(ErrorCode.ANALYSIS_CASE_JOB_POSTING_ALREADY_SELECTED);
        }

        AnalysisCaseSource source = AnalysisCaseSource.builder()
                .analysisCase(analysisCase)
                .extraction(extraction)
                .documentType(documentType)
                .build();
        analysisCaseSourceRepository.save(source);

        return AnalysisCaseSourceResponse.from(source);
    }

    @Transactional
    public void removeSource(Long userId, Long analysisCaseId, Long analysisCaseSourceId) {
        AnalysisCase analysisCase = findOwnedCase(userId, analysisCaseId);
        requireDraft(analysisCase);

        AnalysisCaseSource source = analysisCaseSourceRepository
                .findByAnalysisCaseSourceIdAndAnalysisCase_AnalysisCaseId(analysisCaseSourceId, analysisCaseId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_CASE_SOURCE_NOT_FOUND));
        analysisCaseSourceRepository.delete(source);
    }

    private JobCategory resolveInitialJobCategory(Long userId, Long jobCategoryId) {
        if (jobCategoryId != null) {
            return findJobCategory(jobCategoryId);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        JobCategory defaultJobCategory = user.getDefaultJobCategory();
        if (defaultJobCategory == null) {
            throw new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND);
        }
        return defaultJobCategory;
    }

    private JobCategory findJobCategory(Long jobCategoryId) {
        return jobCategoryRepository.findById(jobCategoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND));
    }

    private List<AnalysisCaseSource> findSources(Long analysisCaseId) {
        return analysisCaseSourceRepository.findByAnalysisCase_AnalysisCaseIdOrderByAnalysisCaseSourceIdAsc(analysisCaseId);
    }

    private AnalysisCase findOwnedCase(Long userId, Long analysisCaseId) {
        return analysisCaseRepository.findByAnalysisCaseIdAndUser_UserId(analysisCaseId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_CASE_NOT_FOUND));
    }

    private void requireDraft(AnalysisCase analysisCase) {
        if (!analysisCase.isDraft()) {
            throw new CustomException(ErrorCode.ANALYSIS_CASE_NOT_DRAFT);
        }
    }
}