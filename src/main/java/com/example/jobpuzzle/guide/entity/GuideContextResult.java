package com.example.jobpuzzle.guide.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "guide_context_result", uniqueConstraints = {
        @UniqueConstraint(name = "uk_guide_context_result_reference", columnNames = {"purpose", "input_reference_type", "input_reference_id"})
})
public class GuideContextResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_context_result_id")
    private Long guideContextResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    private GuideContextPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_reference_type", nullable = false, length = 50)
    private GuideContextInputReferenceType inputReferenceType;

    @Column(name = "input_reference_id", nullable = false, length = 100)
    private String inputReferenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id", nullable = false)
    private JobCategory jobCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "career_level", nullable = false, length = 30)
    private JobCategoryCareerLevel careerLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private JobGuideDocument guide;

    @Column(name = "guide_version", length = 20)
    private String guideVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 50)
    private GuideMatchType matchType;

    @Column(name = "applicable_scope", length = 500)
    private String applicableScope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_focus", columnDefinition = "json")
    private List<String> evaluationFocus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_rules", columnDefinition = "json")
    private List<String> evidenceRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_direction", columnDefinition = "json")
    private List<String> questionDirection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avoid_questions", columnDefinition = "json")
    private List<String> avoidQuestions;

    @Column(name = "fallback_applied", nullable = false)
    private boolean fallbackApplied;

    @Column(name = "insufficient", nullable = false)
    private boolean insufficient;

    @Column(name = "insufficient_reason", length = 500)
    private String insufficientReason;

    // 선택된 가이드의 스냅샷 고정 검색 결과를 저장한다.
    public static GuideContextResult create(User user, String snapshotId, JobCategory jobCategory,
                                            JobGuideDocument guide, GuideMatchType matchType) {
        GuideContextResult result = new GuideContextResult();
        result.user = user;
        result.purpose = GuideContextPurpose.CUSTOMIZED_SYNTHESIS;
        result.inputReferenceType = GuideContextInputReferenceType.ANALYSIS_SNAPSHOT;
        result.inputReferenceId = snapshotId;
        result.jobCategory = jobCategory;
        result.careerLevel = jobCategory.getCareerLevel();
        result.guide = guide;
        result.matchType = matchType;
        result.fallbackApplied = matchType != GuideMatchType.EXACT && matchType != GuideMatchType.NONE;
        result.insufficient = matchType == GuideMatchType.NONE;
        if (guide == null) {
            // NONE context는 서비스 정책상 저장하지 않으므로 호출자에게 불완전 객체를 노출하지 않는다.
            result.insufficientReason = "선택한 직무·경력 또는 공통 범위의 ACTIVE 가이드가 없습니다.";
            return result;
        }
        result.guideVersion = guide.getVersion();
        result.applicableScope = guide.getApplicableScope();
        result.evaluationFocus = guide.getEvaluationFocus();
        result.evidenceRules = guide.getEvidenceRules();
        result.questionDirection = guide.getQuestionDirection();
        result.avoidQuestions = guide.getAvoidQuestions();
        return result;
    }
}
