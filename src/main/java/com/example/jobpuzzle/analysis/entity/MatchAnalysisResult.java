package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "match_analysis_result", uniqueConstraints = {
        @UniqueConstraint(name = "uk_match_analysis_snapshot_key", columnNames = {"snapshot_id", "match_key"}),
        @UniqueConstraint(name = "uk_match_analysis_snapshot_requirement", columnNames = {"snapshot_id", "requirement_id"})
})
public class MatchAnalysisResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private AnalysisInputSnapshot snapshot;

    // AI matchId는 DB PK가 아니라 snapshot 안에서만 유일한 matchKey로 저장한다.
    @Column(name = "match_key", nullable = false, length = 100)
    private String matchKey;

    @Column(name = "requirement_id", nullable = false, length = 100)
    private String requirementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 20)
    private RequirementType requirementType;

    @Column(name = "requirement", nullable = false, length = 500)
    private String requirement;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "posting_source_refs", nullable = false, columnDefinition = "json")
    private List<AnalysisSourceReference> postingSourceRefs;

    @Lob
    @Column(name = "candidate_evidence", columnDefinition = "TEXT")
    private String candidateEvidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "candidate_source_refs", nullable = false, columnDefinition = "json")
    private List<AnalysisSourceReference> candidateSourceRefs;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_level", nullable = false, length = 20)
    private MatchAnalysisResultMatchLevel matchLevel;

    @Lob
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Lob
    @Column(name = "missing_point", columnDefinition = "TEXT")
    private String missingPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id", nullable = false)
    private AiCallLog aiCallLog;

    public static MatchAnalysisResult from(AnalysisInputSnapshot snapshot, AiCallLog aiCallLog,
                                           CustomizedAnalysisGenerationResult.RequirementMatch value) {
        MatchAnalysisResult result = new MatchAnalysisResult();
        result.snapshot = snapshot;
        result.aiCallLog = aiCallLog;
        result.matchKey = value.getMatchId();
        result.requirementId = value.getRequirementId();
        result.requirementType = value.getRequirementType();
        result.requirement = value.getRequirement();
        result.postingSourceRefs = sourceRefs(value.getPostingSourceRefs());
        result.candidateEvidence = value.getCandidateEvidence();
        result.candidateSourceRefs = sourceRefs(value.getCandidateSourceRefs());
        result.matchLevel = value.getMatchLevel();
        result.reason = value.getReason();
        result.missingPoint = value.getMissingPoint();
        return result;
    }

    private static List<AnalysisSourceReference> sourceRefs(List<SourceReference> values) {
        return values == null ? List.of() : values.stream().map(AnalysisSourceReference::from).toList();
    }
}
