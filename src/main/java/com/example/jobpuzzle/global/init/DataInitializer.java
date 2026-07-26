package com.example.jobpuzzle.global.init;

import com.example.jobpuzzle.ai.prompt.PromptTemplate;
import com.example.jobpuzzle.ai.prompt.PromptTemplateRepository;
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
import com.example.jobpuzzle.global.error.CustomException;
import com.example.jobpuzzle.global.error.ErrorCode;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// JobCategorySeeder가 만들어 둔 기준 카테고리를 재사용하므로, 반드시 그 뒤에 실행되어야 함
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.data-initializer",
        name = "enabled",
        havingValue = "true"
)
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
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

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // ddl-auto=update 상태에서 서버를 재실행해도 더미 회원이 중복 저장되지 않도록 제한
        if (userRepository.existsByLoginId(DEMO_LOGIN_ID)) {
            log.info("DataInitializer 건너뜀: 더미 데이터가 이미 존재합니다.");
            return;
        }

        /*
         * 1. 직무 분류 조회 (JobCategorySeeder가 미리 넣어 둔 기준 카테고리를 재사용)
         */

        JobCategory jobCategory = jobCategoryRepository
                .findByMainCategoryAndSubCategoryAndCareerLevel(
                        "IT·개발",
                        "백엔드 개발",
                        JobCategoryCareerLevel.NEW
                )
                .orElseThrow(() -> new CustomException(ErrorCode.JOB_CATEGORY_NOT_FOUND));

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

        log.info(
                "DataInitializer 기본 데이터 완료: userId={}, jobPostingId={}",
                user.getUserId(),
                jobPosting.getJobPostingId()
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

}
