package com.example.jobpuzzle.global.init;

import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 회원가입 / 희망 직무 설정 화면의 대분류-중분류-경력 드롭다운을 채우는 기준 데이터
// 대분류/중분류 목록은 화면 기획에 맞춰 확정된 값 - DB에 없는 조합만 추가함
// (job_category를 참조하는 테이블이 여럿이라 기존 행을 삭제하면 FK 제약 위반이 날 수 있어 삭제는 하지 않음)
// DataInitializer가 이 시더가 만든 기준 카테고리를 조회해서 재사용하므로, 반드시 먼저 실행되어야 함
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JobCategorySeeder implements ApplicationRunner {

    private final JobCategoryRepository jobCategoryRepository;

    private static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put("IT·개발", List.of("백엔드 개발", "프론트엔드 개발", "데이터 분석"));
        CATEGORIES.put("경영·사무", List.of("인사 채용", "마케팅"));
        CATEGORIES.put("디자인", List.of("UX/UI 디자인", "BX 디자인"));
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<String> existingKeys = jobCategoryRepository.findAll().stream()
                .map(JobCategorySeeder::key)
                .collect(Collectors.toSet());

        List<JobCategory> missing = buildTarget().stream()
                .filter(category -> !existingKeys.contains(key(category)))
                .toList();

        if (missing.isEmpty()) {
            log.info("JobCategorySeeder 건너뜀: 기준 직무 카테고리가 이미 모두 존재합니다.");
            return;
        }

        jobCategoryRepository.saveAll(missing);
        log.info("JobCategorySeeder 완료: {}건 추가", missing.size());
    }

    private static String key(JobCategory category) {
        return category.getMainCategory() + "|" + category.getSubCategory() + "|" + category.getCareerLevel();
    }

    private List<JobCategory> buildTarget() {
        List<JobCategory> jobCategories = new ArrayList<>();
        CATEGORIES.forEach((mainCategory, subCategories) ->
                subCategories.forEach(subCategory ->
                        Arrays.stream(JobCategoryCareerLevel.values()).forEach(careerLevel ->
                                jobCategories.add(
                                        JobCategory.builder()
                                                .mainCategory(mainCategory)
                                                .subCategory(subCategory)
                                                .careerLevel(careerLevel)
                                                .build()
                                )
                        )
                )
        );
        return jobCategories;
    }
}
