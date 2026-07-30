package com.example.jobpuzzle.admin.service;

import com.example.jobpuzzle.admin.dto.AdminAiCallLogResponse;
import com.example.jobpuzzle.admin.dto.AdminGuideCreateRequest;
import com.example.jobpuzzle.admin.dto.AdminGuideResponse;
import com.example.jobpuzzle.admin.dto.AdminGuideUsageResponse;
import com.example.jobpuzzle.admin.dto.AdminUserListResponse;
import com.example.jobpuzzle.admin.dto.AdminUserSearchField;
import com.example.jobpuzzle.admin.dto.JobCategoryCreateRequest;
import com.example.jobpuzzle.admin.support.GuideTextChunker;
import com.example.jobpuzzle.ai.log.AiCallLogRepository;
import com.example.jobpuzzle.ai.log.AiCallLogStatus;
import com.example.jobpuzzle.document.extraction.PdfExtractionResult;
import com.example.jobpuzzle.document.extraction.PdfPageResult;
import com.example.jobpuzzle.document.extraction.PdfTextExtractor;
import com.example.jobpuzzle.global.common.dto.PageResponse;
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
import com.example.jobpuzzle.guide.entity.GuideScopeType;
import com.example.jobpuzzle.guide.entity.JobGuideChunk;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import com.example.jobpuzzle.guide.repository.GuideContextResultRepository;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.jobcategory.dto.JobCategoryResponse;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import com.example.jobpuzzle.user.entity.UserStatus;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final GuideContextResultRepository guideContextResultRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final JobGuideDocumentRepository jobGuideDocumentRepository;
    private final JobGuideChunkRepository jobGuideChunkRepository;
    private final PdfTextExtractor pdfTextExtractor;

    @Value("${app.storage.local.base-dir}")
    private String storageBaseDir;

    // 회원 목록/검색
    public PageResponse<AdminUserListResponse> getUserList(
            String keyword, AdminUserSearchField searchField, UserStatus status, Pageable pageable
    ) {
        String searchFieldName = keyword != null ? searchField.name() : null;
        Page<AdminUserListResponse> response = userRepository.search(keyword, searchFieldName, status, pageable)
                .map(AdminUserListResponse::from);
        return PageResponse.from(response);
    }

    // 직무 분류 목록 조회
    public List<JobCategoryResponse> getJobCategories() {
        return jobCategoryRepository.findAll().stream()
                .map(JobCategoryResponse::from)
                .toList();
    }

    // 직무 분류 등록
    public JobCategoryResponse createJobCategory(JobCategoryCreateRequest request) {
        jobCategoryRepository.findByMainCategoryAndSubCategoryAndCareerLevel(
                request.getMainCategory(), request.getSubCategory(), request.getCareerLevel()
        ).ifPresent(existing -> { throw new CustomException(ErrorCode.JOB_CATEGORY_DUPLICATE); });

        JobCategory jobCategory = JobCategory.builder()
                .mainCategory(request.getMainCategory())
                .subCategory(request.getSubCategory())
                .careerLevel(request.getCareerLevel())
                .build();
        return JobCategoryResponse.from(jobCategoryRepository.save(jobCategory));
    }

    // 직무 분류 수정
    public JobCategoryResponse updateJobCategory(Long jobCategoryId, JobCategoryCreateRequest request) {
        JobCategory jobCategory = jobCategoryRepository.findById(jobCategoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND));

        if (jobCategoryRepository.existsByMainCategoryAndSubCategoryAndCareerLevelAndJobCategoryIdNot(
                request.getMainCategory(), request.getSubCategory(), request.getCareerLevel(), jobCategoryId)) {
            throw new CustomException(ErrorCode.JOB_CATEGORY_DUPLICATE);
        }

        jobCategory.update(request.getMainCategory(), request.getSubCategory(), request.getCareerLevel());
        return JobCategoryResponse.from(jobCategory);
    }

    // 직무 분류 삭제 - 공고·가이드·리포트 등 다른 데이터에서 참조 중이면 삭제할 수 없음
    public void deleteJobCategory(Long jobCategoryId) {
        JobCategory jobCategory = jobCategoryRepository.findById(jobCategoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND));
        try {
            jobCategoryRepository.delete(jobCategory);
            jobCategoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.JOB_CATEGORY_IN_USE);
        }
    }

    // 가이드 목록 조회
    public List<AdminGuideResponse> getGuides() {
        return jobGuideDocumentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // 가이드 단건 조회
    public AdminGuideResponse getGuide(Long guideId) {
        JobGuideDocument guide = jobGuideDocumentRepository.findById(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));
        return toResponse(guide);
    }

    private AdminGuideResponse toResponse(JobGuideDocument guide) {
        long chunkCount = jobGuideChunkRepository.countByGuide_GuideId(guide.getGuideId());
        return AdminGuideResponse.from(guide, chunkCount);
    }

    /**
     * 새 가이드 계보의 첫 DRAFT를 생성한다.
     * 버전은 서버가 v1.0으로 고정하고, 적용 범위와 관계없는 요청 필드는 저장 전에 제거한다.
     */
    public AdminGuideResponse createGuide(AdminGuideCreateRequest request, MultipartFile file, User admin) {
        requireAdmin(admin);
        if (request.getGuideCode() == null || request.getGuideCode().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "가이드 코드를 입력해주세요.");
        }
        if (jobGuideDocumentRepository.existsByGuideCode(request.getGuideCode().trim())) {
            throw new CustomException(ErrorCode.GUIDE_CODE_DUPLICATED);
        }
        String version = "v1.0";
        GuideScopeType scopeType = request.getScopeType();
        JobGuideDocument guide = buildGuideAndChunks(
                request, file, admin, request.getGuideCode().trim(), version, null,
                scopeType,
                scopeType == GuideScopeType.CATEGORY ? request.getJobCategoryId() : null,
                scopeType == GuideScopeType.PARENT_CATEGORY ? request.getScopeMainCategory() : null);
        log.info("관리자 가이드 등록 guideId={} guideCode={} version={} sourceType={}",
                guide.getGuideId(), guide.getGuideCode(), guide.getVersion(), guide.getSourceType());
        return toResponse(guide);
    }

    /**
     * 최신 가이드에서만 다음 DRAFT 버전을 생성한다.
     *
     * <p>비관적 잠금과 후속 버전 존재 검사를 함께 사용해 동시 요청에 의한 계보 분기를 막는다.
     * 가이드 코드와 적용 범위는 이전 버전에서 강제 상속해 같은 계보의 검색 범위가 바뀌지 않게 한다.</p>
     */
    public AdminGuideResponse createGuideVersion(Long guideId, AdminGuideCreateRequest request, MultipartFile file, User admin) {
        requireAdmin(admin);
        JobGuideDocument previous = jobGuideDocumentRepository.findWithLockByGuideId(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));
        if (jobGuideDocumentRepository.existsByPreviousGuide_GuideId(previous.getGuideId())) {
            throw new CustomException(ErrorCode.GUIDE_NOT_LATEST_VERSION);
        }

        String nextVersion = bumpVersion(previous.getVersion());
        if (jobGuideDocumentRepository.existsByGuideCodeAndVersion(previous.getGuideCode(), nextVersion)) {
            throw new CustomException(ErrorCode.GUIDE_VERSION_DUPLICATED);
        }

        JobGuideDocument guide = buildGuideAndChunks(
                request, file, admin, previous.getGuideCode(), nextVersion, previous,
                previous.getScopeType(),
                previous.getJobCategory() == null ? null : previous.getJobCategory().getJobCategoryId(),
                previous.getScopeMainCategory());
        log.info("관리자 가이드 새 버전 생성 guideId={} previousGuideId={} guideCode={} version={}",
                guide.getGuideId(), previous.getGuideId(), guide.getGuideCode(), guide.getVersion());
        return toResponse(guide);
    }

    /**
     * 가이드 문서와 검색에 사용할 원문 청크를 같은 트랜잭션에서 저장한다.
     * 저장된 청크는 관리자 검수 후 그대로 임베딩·벡터 색인된다.
     */
    private JobGuideDocument buildGuideAndChunks(
            AdminGuideCreateRequest request, MultipartFile file, User admin,
            String guideCode, String version, JobGuideDocument previousGuide,
            GuideScopeType scopeType,
            Long jobCategoryId, String scopeMainCategory
    ) {
        JobCategory jobCategory = jobCategoryId != null
                ? jobCategoryRepository.findById(jobCategoryId)
                        .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND))
                : null;

        GuideSourceMaterial source = resolveSourceMaterial(request, file);

        JobGuideDocument guide = JobGuideDocument.builder()
                .guideCode(guideCode)
                .previousGuide(previousGuide)
                .scopeType(scopeType)
                .jobCategory(jobCategory)
                .scopeMainCategory(scopeMainCategory)
                .title(request.getTitle())
                .sourceType(request.getSourceType())
                .filePath(source.filePath())
                .version(version)
                .createdBy(admin)
                // 추가 지침은 선택값이지만 기존 DB NOT NULL 계약을 유지하기 위해 빈 문자열로 저장한다.
                .applicableScope(request.getApplicableScope() == null ? "" : request.getApplicableScope().trim())
                .evaluationFocus(request.getEvaluationFocus())
                .evidenceRules(request.getEvidenceRules())
                .questionDirection(request.getQuestionDirection())
                .avoidQuestions(request.getAvoidQuestions())
                .build();
        jobGuideDocumentRepository.save(guide);

        saveChunks(guide, source.text());
        return guide;
    }

    /** 컨트롤러 보안 설정과 별개로 서비스 진입점에서도 관리자 권한을 검증한다. */
    private void requireAdmin(User admin) {
        if (admin == null || admin.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("관리자만 가이드를 관리할 수 있습니다.");
        }
    }

    // PDF면 파일을 저장하고 그 자리에서 텍스트를 추출, 직접입력이면 입력받은 원문을 그대로 사용
    private GuideSourceMaterial resolveSourceMaterial(AdminGuideCreateRequest request, MultipartFile file) {
        if (request.getSourceType() == JobGuideDocumentSourceType.PDF) {
            if (file == null || file.isEmpty()) {
                throw new CustomException(ErrorCode.GUIDE_SOURCE_REQUIRED);
            }
            byte[] bytes = readBytes(file);
            String extractedText = extractPdfText(bytes);
            if (extractedText.isBlank()) {
                throw new CustomException(ErrorCode.GUIDE_SOURCE_REQUIRED);
            }
            // 추출 검증 후에만 파일을 저장해 실패한 등록이 고아 파일을 남기지 않도록 한다.
            String filePath = storeGuideFile(bytes, file.getOriginalFilename());
            return new GuideSourceMaterial(filePath, extractedText);
        }
        if (request.getSourceText() == null || request.getSourceText().isBlank()) {
            throw new CustomException(ErrorCode.GUIDE_SOURCE_REQUIRED);
        }
        return new GuideSourceMaterial(null, request.getSourceText().trim());
    }

    private void saveChunks(JobGuideDocument guide, String text) {
        List<String> pieces = GuideTextChunker.split(text);
        if (pieces.isEmpty()) {
            return;
        }
        List<JobGuideChunk> chunks = new java.util.ArrayList<>();
        for (int index = 0; index < pieces.size(); index++) {
            chunks.add(JobGuideChunk.builder()
                    .guide(guide)
                    .chunkIndex(index)
                    .content(pieces.get(index))
                    .build());
        }
        jobGuideChunkRepository.saveAll(chunks);
    }

    private String extractPdfText(byte[] bytes) {
        PdfExtractionResult result = pdfTextExtractor.extract(new ByteArrayInputStream(bytes));
        StringBuilder text = new StringBuilder();
        for (PdfPageResult page : result.pages()) {
            text.append('[').append(page.pageNumber()).append("페이지]\n");
            text.append(page.text() == null ? "" : page.text()).append("\n\n");
        }
        return text.toString();
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    private record GuideSourceMaterial(String filePath, String text) {
    }

    // "v1.0" -> "v1.1" 처럼 minor 버전만 증가
    private String bumpVersion(String version) {
        var matcher = java.util.regex.Pattern.compile("^v(\\d+)\\.(\\d+)$").matcher(version);
        if (!matcher.matches()) {
            return version + ".1";
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        return "v" + major + "." + (minor + 1);
    }

    /** 현재 활성 가이드를 분석 대상에서 제외하며 서비스 진입점에서도 관리자 권한을 검증한다. */
    public AdminGuideResponse deactivateGuide(Long guideId, User admin) {
        requireAdmin(admin);
        JobGuideDocument guide = jobGuideDocumentRepository.findById(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));
        guide.deactivate();
        log.info("관리자 가이드 비활성화 guideId={} guideCode={} version={}",
                guideId, guide.getGuideCode(), guide.getVersion());
        return toResponse(guide);
    }

    private String storeGuideFile(byte[] bytes, String originalFileName) {
        Path baseDir = Paths.get(storageBaseDir).toAbsolutePath().normalize().resolve("guides");
        String extension = extractExtension(originalFileName);
        String relativePath = "guides/" + UUID.randomUUID() + extension;
        Path targetPath = baseDir.resolve(relativePath.substring("guides/".length()));

        try {
            Files.createDirectories(baseDir);
            Files.write(targetPath, bytes);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }
        return relativePath;
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null) {
            return "";
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        return dotIndex >= 0 ? originalFileName.substring(dotIndex) : "";
    }

    public void getPostingCountByCategory() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void fixMisclassifiedPosting() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    public void manageReportDraft() {
        // TODO: 클래스 정의서 기준으로 구현
    }

    // 가이드 사용 이력 조회
    public PageResponse<AdminGuideUsageResponse> getGuideUsageHistory(Pageable pageable) {
        Page<AdminGuideUsageResponse> response = guideContextResultRepository.findAll(pageable)
                .map(AdminGuideUsageResponse::from);
        return PageResponse.from(response);
    }

    // AI 분석 오류 로그 조회 - status가 없으면 전체 상태
    public PageResponse<AdminAiCallLogResponse> getAiErrorLogs(AiCallLogStatus status, Pageable pageable) {
        Page<AdminAiCallLogResponse> response = aiCallLogRepository.search(status, pageable)
                .map(AdminAiCallLogResponse::from);
        return PageResponse.from(response);
    }
}
