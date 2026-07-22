package com.example.jobpuzzle.global.init;

import com.example.jobpuzzle.ai.client.MockAiClient;
import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.ai.log.AiCallLogResultType;
import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.ai.log.AiCallLogRepository;
import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
import com.example.jobpuzzle.analysis.entity.CandidateMaterialAnalysis;
import com.example.jobpuzzle.analysis.entity.CandidateMaterialAnalysisSource;
import com.example.jobpuzzle.analysis.entity.ConfirmedAnalysisSnapshot;
import com.example.jobpuzzle.analysis.entity.JobPostingAnalysis;
import com.example.jobpuzzle.analysis.repository.CandidateMaterialAnalysisRepository;
import com.example.jobpuzzle.analysis.repository.ConfirmedAnalysisSnapshotRepository;
import com.example.jobpuzzle.analysis.repository.JobPostingAnalysisRepository;
import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentExtractionStatus;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.document.repository.DocumentExtractionRepository;
import com.example.jobpuzzle.document.repository.UserDocumentRepository;
import com.example.jobpuzzle.guide.entity.JobGuideChunk;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.guide.entity.JobGuideDocumentSourceType;
import com.example.jobpuzzle.guide.repository.JobGuideChunkRepository;
import com.example.jobpuzzle.guide.repository.JobGuideDocumentRepository;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.jobposting.entity.Company;
import com.example.jobpuzzle.jobposting.entity.JobPosting;
import com.example.jobpuzzle.jobposting.entity.JobPostingCareerLevel;
import com.example.jobpuzzle.jobposting.repository.CompanyRepository;
import com.example.jobpuzzle.jobposting.repository.JobPostingRepository;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.data-initializer",
        name = "enabled",
        havingValue = "true"
)
public class DataInitializer implements ApplicationRunner {

    private static final String DEMO_LOGIN_ID = "demo_user";

    private final JobCategoryRepository jobCategoryRepository;
    private final UserRepository userRepository;

    private final UserDocumentRepository userDocumentRepository;
    private final DocumentExtractionRepository documentExtractionRepository;

    private final CompanyRepository companyRepository;
    private final JobPostingRepository jobPostingRepository;

    private final PromptTemplateRepository promptTemplateRepository;

    private final JobGuideDocumentRepository jobGuideDocumentRepository;
    private final JobGuideChunkRepository jobGuideChunkRepository;

    private final AiCallLogRepository aiCallLogRepository;

    private final JobPostingAnalysisRepository jobPostingAnalysisRepository;
    private final CandidateMaterialAnalysisRepository candidateMaterialAnalysisRepository;
    private final ConfirmedAnalysisSnapshotRepository confirmedAnalysisSnapshotRepository;

    private final PasswordEncoder passwordEncoder;
    private final MockAiClient mockAiClient;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // ddl-auto=update 상태에서 서버를 재실행해도 더미 회원이 중복 저장되지 않도록 제한
        if (userRepository.existsByLoginId(DEMO_LOGIN_ID)) {
            log.info("DataInitializer 건너뜀: 더미 데이터가 이미 존재합니다.");
            return;
        }

        /*
         * 1. 직무 분류 생성
         */

        JobCategory jobCategory = JobCategory.builder()
                .mainCategory("IT·개발")
                .subCategory("백엔드 개발자")
                .careerLevel(JobCategoryCareerLevel.NEW)
                .build();

        jobCategoryRepository.save(jobCategory);

        /*
         * 2. 사용자 생성
         */

        User user = User.createLocalUser(
                DEMO_LOGIN_ID,
                passwordEncoder.encode("test1234"),
                "demo@jobpuzzle.com",
                "테스트 사용자",
                jobCategory
        );

        userRepository.save(user);

        /*
         * 3. 사용자 문서 생성
         */

        UserDocument jobPostingDocument = createTextDocument(
                user,
                UserDocumentType.JOB_POSTING,
                "테스트 채용공고"
        );

        UserDocument companyInfoDocument = createTextDocument(
                user,
                UserDocumentType.COMPANY_INFO,
                "테스트 회사정보"
        );

        UserDocument resumeDocument = createTextDocument(
                user,
                UserDocumentType.RESUME,
                "테스트 이력서"
        );

        UserDocument coverLetterDocument = createTextDocument(
                user,
                UserDocumentType.COVER_LETTER,
                "테스트 자기소개서"
        );

        UserDocument portfolioDocument = createTextDocument(
                user,
                UserDocumentType.PORTFOLIO,
                "테스트 포트폴리오"
        );

        UserDocument experienceNoteDocument = createTextDocument(
                user,
                UserDocumentType.EXPERIENCE_NOTE,
                "테스트 경험정리"
        );

        userDocumentRepository.saveAll(List.of(
                jobPostingDocument,
                companyInfoDocument,
                resumeDocument,
                coverLetterDocument,
                portfolioDocument,
                experienceNoteDocument
        ));

        /*
         * 4. 문서 추출 결과 생성
         */

        DocumentExtraction jobPostingExtraction = createExtraction(
                jobPostingDocument,
                """
                주요 업무
                - Spring Boot 기반 백엔드 API 개발
                - 데이터베이스 설계 및 성능 개선

                자격 요건
                - Java 및 Spring Boot 개발 경험
                - 관계형 데이터베이스 설계 경험

                우대 사항
                - AWS 배포 경험
                - 테스트 코드 작성 경험
                """
        );

        DocumentExtraction companyInfoExtraction = createExtraction(
                companyInfoDocument,
                """
                테스트 주식회사는 사용자 중심의 서비스를 개발하는 IT 기업입니다.
                주도적인 문제 해결과 협업 커뮤니케이션을 중요하게 평가합니다.
                """
        );

        DocumentExtraction resumeExtraction = createExtraction(
                resumeDocument,
                """
                Java와 Spring Boot를 활용하여 팀 프로젝트의 백엔드 API를 개발했습니다.
                JPA와 MySQL을 사용하여 회원과 게시물 데이터를 설계했습니다.
                """
        );

        DocumentExtraction coverLetterExtraction = createExtraction(
                coverLetterDocument,
                """
                백엔드 개발 과정에서 문제를 구조적으로 분석하고 해결하는 경험을 쌓았습니다.
                팀원과 API 계약을 협의하여 기능 통합 과정의 오류를 줄였습니다.
                """
        );

        DocumentExtraction portfolioExtraction = createExtraction(
                portfolioDocument,
                """
                프로젝트명: 학습 관리 플랫폼
                담당 역할: 회원가입과 로그인 API 개발
                사용 기술: Spring Boot, JPA, MySQL
                """
        );

        DocumentExtraction experienceNoteExtraction = createExtraction(
                experienceNoteDocument,
                """
                상황: 조회 요청 증가로 응답 속도가 느려졌습니다.
                과제: 조회 성능을 개선해야 했습니다.
                행동: 쿼리와 인덱스 구조를 점검했습니다.
                결과: 평균 응답 시간을 줄였습니다.
                """
        );

        documentExtractionRepository.saveAll(List.of(
                jobPostingExtraction,
                companyInfoExtraction,
                resumeExtraction,
                coverLetterExtraction,
                portfolioExtraction,
                experienceNoteExtraction
        ));

        /*
         * 5. 회사 생성
         */

        Company company = Company.builder()
                .companyName("테스트 주식회사")
                .industry("IT 서비스")
                .build();

        companyRepository.save(company);

        /*
         * 6. 채용공고 생성
         */

        JobPosting jobPosting = JobPosting.builder()
                .user(user)
                .company(company)
                .document(jobPostingDocument)
                .companyInfoDocument(companyInfoDocument)
                .jobCategory(jobCategory)
                .jobTitle("백엔드 개발자")
                .careerLevel(JobPostingCareerLevel.NEW)
                .build();

        jobPostingRepository.save(jobPosting);

        /*
         * 7. 프롬프트 템플릿 생성
         */

        PromptTemplate jobPostingPrompt = PromptTemplate.builder()
                .promptCode("PT-JOB-001")
                .name("채용공고 분석 프롬프트")
                .version("v1.0")
                .targetJson("JSON-01")
                .templateText(
                        """
                        입력된 채용공고에서 주요 업무, 자격 요건, 우대 사항,
                        회사 가치와 핵심 역량을 추출하여 JSON-01 형식으로 반환한다.
                        """
                )
                .forbiddenRules(
                        """
                        입력 자료에 없는 내용을 임의로 생성하지 않는다.
                        확인되지 않은 경력 요건을 단정하지 않는다.
                        """
                )
                .isActive(true)
                .build();

        PromptTemplate candidatePrompt = PromptTemplate.builder()
                .promptCode("PT-CAND-001")
                .name("지원자 자료 분석 프롬프트")
                .version("v1.0")
                .targetJson("JSON-02")
                .templateText(
                        """
                        이력서, 자기소개서, 포트폴리오와 경험 자료에서
                        경험, 기술, 역할, 성과와 STAR 후보를 추출하여
                        JSON-02 형식으로 반환한다.
                        """
                )
                .forbiddenRules(
                        """
                        지원자 자료에 존재하지 않는 경험과 성과를 생성하지 않는다.
                        추정 정보는 확정 사실처럼 표현하지 않는다.
                        """
                )
                .isActive(true)
                .build();

        promptTemplateRepository.saveAll(List.of(
                jobPostingPrompt,
                candidatePrompt
        ));

        /*
         * 8. 직무 가이드 및 청크 생성
         */

        JobGuideDocument guide = JobGuideDocument.builder()
                .jobCategory(jobCategory)
                .title("백엔드 개발자 면접 분석 가이드")
                .sourceType(JobGuideDocumentSourceType.DIRECT_INPUT)
                .filePath(null)
                .version("v1.0")
                .createdBy(user)
                .build();

        // Builder 생성 직후 상태는 DRAFT이므로 테스트용 활성 가이드로 전환
        guide.activate();

        jobGuideDocumentRepository.save(guide);

        JobGuideChunk guideChunk1 = JobGuideChunk.builder()
                .guide(guide)
                .chunkIndex(0)
                .title("백엔드 기본 역량")
                .content(
                        """
                        백엔드 개발자는 HTTP 요청과 응답 구조,
                        REST API 설계, 데이터베이스 모델링과
                        예외 처리 구조를 설명할 수 있어야 한다.
                        """
                )
                .contentSummary("REST API와 데이터베이스 설계 기본 역량")
                .embeddingRef(null)
                .build();

        JobGuideChunk guideChunk2 = JobGuideChunk.builder()
                .guide(guide)
                .chunkIndex(1)
                .title("경험 검증 기준")
                .content(
                        """
                        프로젝트 경험은 문제 상황, 담당 역할,
                        해결 과정과 결과가 구분되어야 하며
                        지원자가 직접 수행한 범위가 확인되어야 한다.
                        """
                )
                .contentSummary("프로젝트 경험과 본인 기여도 확인 기준")
                .embeddingRef(null)
                .build();

        jobGuideChunkRepository.saveAll(List.of(
                guideChunk1,
                guideChunk2
        ));

        /*
         * 9. Mock 채용공고 분석
         */

        AiCallLog jobPostingCallLog = AiCallLog.builder()
                .provider(AiProvider.MOCK)
                .model("mock-v1")
                .promptTemplate(jobPostingPrompt)
                .promptVersion(jobPostingPrompt.getVersion())
                .guide(null)
                .guideVersion(null)
                .resultType(AiCallLogResultType.JOB_POSTING_ANALYSIS)
                .build();

        aiCallLogRepository.save(jobPostingCallLog);

        JobPostingAnalysisResult jobPostingResult =
                mockAiClient.analyzeJobPosting("DataInitializer 채용공고 분석");

        JobPostingAnalysis jobPostingAnalysis = JobPostingAnalysis.builder()
                .jobPosting(jobPosting)
                .jobCategory(jobCategory)
                .mainTasks(jobPostingResult.getMainTasks())
                .requirements(jobPostingResult.getRequirements())
                .preferred(jobPostingResult.getPreferred())
                .companyValues(jobPostingResult.getCompanyValues())
                .coreCompetencies(jobPostingResult.getCoreCompetencies())
                .missingEvidence(jobPostingResult.getMissingEvidence())
                .aiCallLog(jobPostingCallLog)
                .build();

        jobPostingAnalysisRepository.save(jobPostingAnalysis);

        // 분석 결과 저장 후 다형 참조 resultId를 연결
        jobPostingCallLog.complete(jobPostingAnalysis.getAnalysisId());

        /*
         * 10. Mock 지원자 자료 분석
         */

        AiCallLog candidateCallLog = AiCallLog.builder()
                .provider(AiProvider.MOCK)
                .model("mock-v1")
                .promptTemplate(candidatePrompt)
                .promptVersion(candidatePrompt.getVersion())
                .guide(null)
                .guideVersion(null)
                .resultType(AiCallLogResultType.CANDIDATE_MATERIAL_ANALYSIS)
                .build();

        aiCallLogRepository.save(candidateCallLog);

        CandidateMaterialAnalysisResult candidateResult =
                mockAiClient.analyzeCandidateMaterial(
                        "DataInitializer 지원자 자료 분석"
                );

        /*
         * CandidateMaterialAnalysis에는 AI가 여러 사용자 자료를 종합하여 만든
         * 분석 결과만 저장한다.
         *
         * 실제 분석에 사용한 이력서·자기소개서·포트폴리오·경험노트는
         * CandidateMaterialAnalysisSource를 통해 별도로 여러 건 연결한다.
         */
        CandidateMaterialAnalysis candidateAnalysis =
                CandidateMaterialAnalysis.builder()
                        .user(user)
                        .jobCategory(jobCategory)

                        // AI 응답 DTO를 DB JSON 저장용 객체로 변환
                        .resumeAnalysis(
                                CandidateMaterialAnalysis.Resume.from(
                                        candidateResult.getResume()
                                )
                        )
                        .coverLetterAnalysis(
                                CandidateMaterialAnalysis.CoverLetter.from(
                                        candidateResult.getCoverLetter()
                                )
                        )
                        .portfolioAnalysis(
                                CandidateMaterialAnalysis.Portfolio.from(
                                        candidateResult.getPortfolio()
                                )
                        )
                        .experienceNoteAnalysis(
                                CandidateMaterialAnalysis.ExperienceNote.from(
                                        candidateResult.getExperienceNote()
                                )
                        )
                        .missingEvidence(candidateResult.getMissingEvidence())
                        .aiCallLog(candidateCallLog)
                        .build();

        /*
         * 분석에 사용한 원본 문서와 확정 추출 결과를 Source 엔티티로 연결한다.
         *
         * CandidateMaterialAnalysis의 sources에 cascade = CascadeType.ALL이 있으므로
         * candidateAnalysis만 저장해도 Source 엔티티까지 함께 저장된다.
         */
        addCandidateAnalysisSource(
                candidateAnalysis,
                resumeDocument,
                resumeExtraction
        );

        addCandidateAnalysisSource(
                candidateAnalysis,
                coverLetterDocument,
                coverLetterExtraction
        );

        addCandidateAnalysisSource(
                candidateAnalysis,
                portfolioDocument,
                portfolioExtraction
        );

        addCandidateAnalysisSource(
                candidateAnalysis,
                experienceNoteDocument,
                experienceNoteExtraction
        );

        candidateMaterialAnalysisRepository.save(candidateAnalysis);

        // 분석 결과 저장 후 다형 참조 resultId를 연결
        candidateCallLog.complete(candidateAnalysis.getAnalysisId());

        /*
         * 11. 확정 분석 스냅샷 생성
         */

        ConfirmedAnalysisSnapshot snapshot =
                ConfirmedAnalysisSnapshot.builder()
                        .jobPostingAnalysis(jobPostingAnalysis)
                        .candidateAnalysis(candidateAnalysis)
                        .confirmedBy(user)
                        .build();

        confirmedAnalysisSnapshotRepository.save(snapshot);

        log.info(
                "DataInitializer 완료: userId={}, jobPostingId={}, snapshotId={}",
                user.getUserId(),
                jobPosting.getJobPostingId(),
                snapshot.getSnapshotId()
        );
    }

    // TEXT 입력 방식의 사용자 문서 생성
    private UserDocument createTextDocument(
            User user,
            UserDocumentType documentType,
            String displayName
    ) {
        return UserDocument.builder()
                .user(user)
                .documentType(documentType)
                .sourceType(UserDocumentSourceType.TEXT)
                .displayName(displayName)
                .filePath(null)
                .fileName(null)
                .keepOriginal(false)
                .build();
    }

    // 사용자 문서에서 정상 추출되어 확정 완료된 테스트 버전 생성
    private DocumentExtraction createExtraction(
            UserDocument document,
            String content
    ) {
        DocumentExtraction extraction = DocumentExtraction.builder()
                .document(document)
                .baseExtraction(null)
                .extractionStatus(DocumentExtractionStatus.SUCCESS)
                .versionStatus(DocumentVersionStatus.DRAFT)
                .content(content)
                .pageCount(null)
                .ocrApplied(false)
                .failureReason(null)
                .build();
        extraction.confirm();
        return extraction;
    }

    /*
     * 한 번의 사용자 자료 분석에 사용된 원본 문서와 확정 추출 결과를
     * CandidateMaterialAnalysisSource로 생성하여 분석 결과에 연결한다.
     *
     * CandidateMaterialAnalysis의 sources에 CascadeType.ALL이 적용되어 있으므로
     * 부모 분석 결과를 저장하면 연결된 Source도 함께 저장된다.
     */
    private void addCandidateAnalysisSource(
            CandidateMaterialAnalysis analysis,
            UserDocument document,
            DocumentExtraction extraction
    ) {
        // 해당 자료가 없으면 Source를 생성하지 않는다.
        if (document == null || extraction == null) {
            return;
        }

        CandidateMaterialAnalysisSource source =
                CandidateMaterialAnalysisSource.builder()
                        // 어느 사용자 자료 분석 결과에 사용됐는지 연결
                        .analysis(analysis)

                        // 사용한 원본 문서
                        .document(document)

                        // 실제 AI 분석에 사용한 확정 추출 결과
                        .extraction(extraction)
                        .build();

        // 부모 엔티티의 sources 목록에 추가
        analysis.addSource(source);
    }
}