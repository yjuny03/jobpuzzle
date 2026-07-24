package com.example.jobpuzzle.interview.entity;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.guide.entity.GuideContextResult;
import com.example.jobpuzzle.guide.entity.JobGuideDocument;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interview_session")
public class InterviewSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false)
    private QuestionSet questionSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private AnalysisInputSnapshot snapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 30)
    private InterviewSessionMode mode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id", nullable = false)
    private JobCategory jobCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "career_level", nullable = false, length = 20)
    private JobCategoryCareerLevel careerLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_context_id")
    private GuideContextResult guideContextResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private JobGuideDocument guideDocument;

    @Column(name = "guide_version", length = 20)
    private String guideVersion;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "target_weakness_tag", length = 100)
    private String targetWeaknessTag;

    @Column(name = "target_dimension", length = 50)
    private String targetDimension;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "basis_evaluation_ids", columnDefinition = "json")
    private List<Long> basisEvaluationIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InterviewSessionStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static InterviewSession create(QuestionSet questionSet) {
        InterviewSession session = new InterviewSession();
        session.user = questionSet.getUser();
        session.questionSet = questionSet;
        session.snapshot = questionSet.getSnapshot();
        session.mode = questionSet.getInterviewMode();
        session.jobCategory = questionSet.getJobCategory();
        session.careerLevel = questionSet.getCareerLevel();
        session.guideContextResult = questionSet.getGuideContextResult();
        if (questionSet.getGuideContextResult() != null) {
            session.guideDocument = questionSet.getGuideContextResult().getGuide();
            session.guideVersion = questionSet.getGuideContextResult().getGuideVersion();
        }
        session.promptVersion = questionSet.getPromptVersion();
        session.targetWeaknessTag = questionSet.getTargetWeaknessTag();
        session.targetDimension = questionSet.getTargetDimension();
        session.basisEvaluationIds = questionSet.getBasisEvaluationIds() == null
                ? List.of()
                : List.copyOf(questionSet.getBasisEvaluationIds());
        session.status = InterviewSessionStatus.CREATED;
        return session;
    }

    public void start() {
        if (status == InterviewSessionStatus.CREATED) {
            status = InterviewSessionStatus.IN_PROGRESS;
            startedAt = LocalDateTime.now();
        }
    }

    public void complete() {
        status = InterviewSessionStatus.COMPLETED;
        completedAt = LocalDateTime.now();
    }

    public void cancel() {
        status = InterviewSessionStatus.CANCELED;
        canceledAt = LocalDateTime.now();
    }

    public boolean isEditable() {
        return status == InterviewSessionStatus.CREATED || status == InterviewSessionStatus.IN_PROGRESS;
    }

    public void softDelete() {
        deletedAt = LocalDateTime.now();
    }
}
