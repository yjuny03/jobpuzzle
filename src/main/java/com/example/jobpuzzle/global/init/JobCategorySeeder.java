package com.example.jobpuzzle.global.init;

import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.jobcategory.repository.JobCategoryRepository;
import com.example.jobpuzzle.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 회원가입 / 희망 직무 설정 화면의 대분류-중분류-경력 드롭다운을 채우는 기준 데이터
// 대분류/중분류 목록은 화면 기획에 맞춰 확정된 값 - 현재 DB 내용이 이 목록과 다르면 통째로 교체함
@Slf4j
@Component
@RequiredArgsConstructor
public class JobCategorySeeder implements ApplicationRunner {

    private final JobCategoryRepository jobCategoryRepository;
    private final UserRepository userRepository;

    private static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put("IT·개발", List.of("백엔드 개발", "프론트엔드 개발", "데이터 분석"));
        CATEGORIES.put("경영·사무", List.of("인사 채용", "마케팅"));
        CATEGORIES.put("디자인", List.of("UX/UI 디자인", "BX 디자인"));
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<JobCategory> target = buildTarget();

        if (jobCategoryRepository.count() == target.size()) {
            log.info("JobCategorySeeder 건너뜀: 이미 목표 직무 카테고리 데이터가 있습니다.");
            return;
        }

        // 기존 직무 카테고리를 참조 중인 회원이 있으면 FK 제약 때문에 삭제가 실패하므로, 재시딩 전에 참조부터 끊어둠
        userRepository.findAll().stream()
                .filter(user -> user.getDefaultJobCategory() != null)
                .forEach(user -> user.updateProfile(user.getName(), user.getEmail(), null));

        jobCategoryRepository.deleteAll();
        jobCategoryRepository.saveAll(target);
        log.info("JobCategorySeeder 완료: {}건으로 교체", target.size());
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
