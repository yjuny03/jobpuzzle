package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.guide.entity.GuideContextResult;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "question_set", uniqueConstraints = {
        @UniqueConstraint(name = "uk_question_set_snapshot_mode", columnNames = {"snapshot_id", "mode"})
})
public class QuestionSet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_set_id")
    private Long questionSetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // QuestionSet은 세션 생성 전에 PASS 질문을 고정하는 분석 결과다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private AnalysisInputSnapshot snapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 30)
    private InterviewSessionMode interviewMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id", nullable = false)
    private JobCategory jobCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "career_level", nullable = false, length = 20)
    private JobCategoryCareerLevel careerLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_context_id")
    private GuideContextResult guideContextResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_source", nullable = false, length = 30)
    private QuestionSetGenerationSource generationSource;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuestionSetStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id", nullable = false)
    private AiCallLog aiCallLog;

    public static QuestionSet companyFit(AnalysisInputSnapshot snapshot, GuideContextResult guideContextResult,
                                         AiCallLog aiCallLog) {
        QuestionSet questionSet = new QuestionSet();
        questionSet.user = snapshot.getUser();
        questionSet.snapshot = snapshot;
        questionSet.interviewMode = InterviewSessionMode.COMPANY_FIT;
        questionSet.jobCategory = snapshot.getJobCategory();
        questionSet.careerLevel = snapshot.getJobCategory().getCareerLevel();
        questionSet.guideContextResult = guideContextResult;
        questionSet.generationSource = QuestionSetGenerationSource.AI;
        questionSet.promptVersion = aiCallLog.getPromptVersion();
        questionSet.status = QuestionSetStatus.ACTIVE;
        questionSet.aiCallLog = aiCallLog;
        return questionSet;
    }
}
