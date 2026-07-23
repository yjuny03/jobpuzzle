package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.CustomizedAnalysisGenerationResult;
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
@Table(name = "readiness_result")
public class ReadinessResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "readiness_id")
    private Long readinessId;

    // readiness는 입력 스냅샷마다 하나만 보존한다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false, unique = true)
    private AnalysisInputSnapshot snapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReadinessResultStatus status;

    @Column(name = "can_generate_questions", nullable = false)
    private boolean canGenerateQuestions;

    @Lob
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "limitations", nullable = false, columnDefinition = "json")
    private List<String> limitations = List.of();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id", nullable = false)
    private AiCallLog aiCallLog;

    public static ReadinessResult from(AnalysisInputSnapshot snapshot, AiCallLog aiCallLog,
                                       CustomizedAnalysisGenerationResult.Readiness value) {
        ReadinessResult result = new ReadinessResult();
        result.snapshot = snapshot;
        result.aiCallLog = aiCallLog;
        result.status = value.getStatus();
        result.canGenerateQuestions = value.isCanGenerateQuestions();
        result.reason = value.getReason();
        result.limitations = value.getLimitations() == null ? List.of() : List.copyOf(value.getLimitations());
        return result;
    }
}
