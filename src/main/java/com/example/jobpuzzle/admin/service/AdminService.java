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
import com.example.jobpuzzle.guide.entity.JobGuideChunk;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentStatus;
import com.example.jobpuzzle.guide.repository.GuideContextResultRepository;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.jobcategory.dto.JobCategoryResponse;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserStatus;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 가이드 등록 - file은 sourceType=PDF일 때만 사용
    public AdminGuideResponse createGuide(AdminGuideCreateRequest request, MultipartFile file, User admin) {
        if (request.getGuideCode() == null || request.getGuideCode().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "가이드 코드를 입력해주세요.");
        }
        String version = "v1.0";
        if (jobGuideDocumentRepository.existsByGuideCodeAndVersion(request.getGuideCode(), version)) {
            throw new CustomException(ErrorCode.GUIDE_CODE_VERSION_DUPLICATE);
        }
        JobGuideDocument guide = buildGuideAndChunks(request, file, admin, request.getGuideCode(), version, null);
        return toResponse(guide);
    }

    // 가이드 새 버전 생성 - guideCode는 이전 버전에서 상속하고 버전만 자동 증가
    public AdminGuideResponse createGuideVersion(Long guideId, AdminGuideCreateRequest request, MultipartFile file, User admin) {
        JobGuideDocument previous = jobGuideDocumentRepository.findById(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));

        String nextVersion = bumpVersion(previous.getVersion());
        if (jobGuideDocumentRepository.existsByGuideCodeAndVersion(previous.getGuideCode(), nextVersion)) {
            throw new CustomException(ErrorCode.GUIDE_CODE_VERSION_DUPLICATE);
        }

        JobGuideDocument guide = buildGuideAndChunks(request, file, admin, previous.getGuideCode(), nextVersion, previous);
        return toResponse(guide);
    }

    // 가이드 저장 + (원문이 있으면) 텍스트 추출과 청크 분할까지 한 트랜잭션으로 처리
    private JobGuideDocument buildGuideAndChunks(
            AdminGuideCreateRequest request, MultipartFile file, User admin,
            String guideCode, String version, JobGuideDocument previousGuide
    ) {
        JobCategory jobCategory = request.getJobCategoryId() != null
                ? jobCategoryRepository.findById(request.getJobCategoryId())
                        .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND))
                : null;

        GuideSourceMaterial source = resolveSourceMaterial(request, file);

        JobGuideDocument guide = JobGuideDocument.builder()
                .guideCode(guideCode)
                .previousGuide(previousGuide)
                .scopeType(request.getScopeType())
                .jobCategory(jobCategory)
                .scopeMainCategory(request.getScopeMainCategory())
                .title(request.getTitle())
                .sourceType(request.getSourceType())
                .filePath(source.filePath())
                .version(version)
                .createdBy(admin)
                .applicableScope(request.getApplicableScope())
                .evaluationFocus(request.getEvaluationFocus())
                .evidenceRules(request.getEvidenceRules())
                .questionDirection(request.getQuestionDirection())
                .avoidQuestions(request.getAvoidQuestions())
                .build();
        jobGuideDocumentRepository.save(guide);

        saveChunks(guide, source.text());
        return guide;
    }

    // PDF면 파일을 저장하고 그 자리에서 텍스트를 추출, 직접입력이면 입력받은 원문을 그대로 사용
    private GuideSourceMaterial resolveSourceMaterial(AdminGuideCreateRequest request, MultipartFile file) {
        if (request.getSourceType() == JobGuideDocumentSourceType.PDF && file != null && !file.isEmpty()) {
            byte[] bytes = readBytes(file);
            String filePath = storeGuideFile(bytes, file.getOriginalFilename());
            String extractedText = extractPdfText(bytes);
            return new GuideSourceMaterial(filePath, extractedText);
        }
        return new GuideSourceMaterial(null, request.getSourceText());
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

    // 가이드 활성화 - 같은 범위에 이미 활성화된 가이드가 있으면 force=true일 때만 그 가이드를 비활성화하고 진행
    public AdminGuideResponse activateGuide(Long guideId, boolean force) {
        JobGuideDocument guide = jobGuideDocumentRepository.findById(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));

        List<JobGuideDocument> conflicts = findActiveConflicts(guide);
        if (!conflicts.isEmpty() && !force) {
            JobGuideDocument conflict = conflicts.get(0);
            throw new CustomException(ErrorCode.GUIDE_ACTIVE_DUPLICATED,
                    "이미 활성화된 가이드가 있습니다: [" + conflict.getGuideCode() + " " + conflict.getVersion() + "] "
                            + conflict.getTitle() + ". 계속하면 이 가이드는 비활성화되고 지금 가이드가 새로 활성화됩니다.");
        }

        conflicts.forEach(JobGuideDocument::deactivate);
        guide.activate();
        return toResponse(guide);
    }

    // 가이드 비활성화
    public AdminGuideResponse deactivateGuide(Long guideId) {
        JobGuideDocument guide = jobGuideDocumentRepository.findById(guideId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_NOT_FOUND));
        guide.deactivate();
        return toResponse(guide);
    }

    // 같은 검색 범위(scope)에서 이 가이드를 제외하고 이미 ACTIVE인 가이드를 찾는다
    private List<JobGuideDocument> findActiveConflicts(JobGuideDocument guide) {
        List<JobGuideDocument> found = switch (guide.getScopeType()) {
            case CATEGORY -> jobGuideDocumentRepository
                    .findByScopeTypeAndJobCategory_MainCategoryAndJobCategory_SubCategoryAndJobCategory_CareerLevelAndStatus(
                            guide.getScopeType(),
                            guide.getJobCategory().getMainCategory(),
                            guide.getJobCategory().getSubCategory(),
                            guide.getJobCategory().getCareerLevel(),
                            JobGuideDocumentStatus.ACTIVE
                    );
            case PARENT_CATEGORY -> jobGuideDocumentRepository.findByScopeTypeAndScopeMainCategoryAndStatus(
                    guide.getScopeType(), guide.getScopeMainCategory(), JobGuideDocumentStatus.ACTIVE);
            case GLOBAL_COMMON -> jobGuideDocumentRepository.findByScopeTypeAndStatus(
                    guide.getScopeType(), JobGuideDocumentStatus.ACTIVE);
        };
        return found.stream().filter(g -> !g.getGuideId().equals(guide.getGuideId())).toList();
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