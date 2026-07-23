package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 세션 생성 전에 저장되는 모드별 질문 묶음. 사용자가 질문을 선택한 뒤 세션 생성
@Getter
@Entity
@NoArgsConstructor
@Table(name = "question_set")
public class QuestionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionSetId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewSessionMode mode;

    // COMPANY_FIT에서 필수. 다른 모드는 NULL
    private Long snapshotId;

    @Column(nullable = false)
    private Long jobCategoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobCategoryCareerLevel careerLevel;

    private Long guideContextId;

    // WEAKNESS_REVIEW 모드의 약점 태그
    private String targetWeaknessTag;

    // WEAKNESS_REVIEW 모드의 단일 평가 관점
    private String targetDimension;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionSetGenerationSource generationSource;

    // AI 생성 시 프롬프트 버전. fallback 템플릿 사용 시 NULL
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionSetStatus status;

    // 성공 호출 로그. fallback 템플릿 사용 시 실패 로그는 별도 보존하고 이 값은 NULL
    private Long aiCallLogId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private QuestionSet(
            Long userId,
            InterviewSessionMode mode,
            Long snapshotId,
            Long jobCategoryId,
            JobCategoryCareerLevel careerLevel,
            Long guideContextId,
            String targetWeaknessTag,
            String targetDimension,
            QuestionSetGenerationSource generationSource,
            String promptVersion,
            Long aiCallLogId
    ) {
        this.userId = userId;
        this.mode = mode;
        this.snapshotId = snapshotId;
        this.jobCategoryId = jobCategoryId;
        this.careerLevel = careerLevel;
        this.guideContextId = guideContextId;
        this.targetWeaknessTag = targetWeaknessTag;
        this.targetDimension = targetDimension;
        this.generationSource = generationSource == null ? QuestionSetGenerationSource.AI : generationSource;
        this.promptVersion = promptVersion;
        this.aiCallLogId = aiCallLogId;

        this.status = QuestionSetStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void archive() {
        this.status = QuestionSetStatus.ARCHIVED;
    }
}
