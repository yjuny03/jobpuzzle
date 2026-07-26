package com.example.jobpuzzle.analysis.rag.entity;

import com.example.jobpuzzle.analysis.entity.AnalysisInputSnapshot;
import com.example.jobpuzzle.analysis.entity.RequirementType;
import com.example.jobpuzzle.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** JSON-01 requirement 하나에 대해 snapshot 기준으로 고정한 candidate 검색 계약이다. */
@Getter
@Entity
@NoArgsConstructor
@Table(name = "requirement_retrieval_result", uniqueConstraints = {
        @UniqueConstraint(name = "uk_requirement_retrieval_snapshot_requirement_corpus",
                columnNames = {"snapshot_id", "requirement_id", "corpus_type"})
})
public class RequirementRetrievalResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "retrieval_result_id")
    private Long retrievalResultId;

    // 검색 결과가 재현해야 하는 분석 입력 snapshot을 고정한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private AnalysisInputSnapshot snapshot;

    // snapshot 소유자를 저장해 결과 조회의 사용자 격리를 단순하게 한다.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // JSON-01 내부 requirement ID는 DB PK와 별개인 재사용 식별자다.
    @Column(name = "requirement_id", nullable = false, length = 100)
    private String requirementId;

    // 필수·우대 requirement 구분을 고정해 동일 ID 계약을 명확히 한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 20)
    private RequirementType requirementType;

    // 검색 시점의 JSON-01 requirement 원문을 보존한다.
    @Column(name = "requirement_text", nullable = false, length = 1000)
    private String requirementText;

    // 이번 단계의 candidate corpus를 명시해 향후 guide retrieval과 분리한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "corpus_type", nullable = false, length = 30)
    private RetrievalCorpusType corpusType;

    // 실제 VectorSearchPort에 전달한 결정적 검색 문자열이다.
    @Column(name = "query_text", nullable = false, length = 1500)
    private String queryText;

    // query·검색 계약을 SHA-256으로 고정해 기존 결과 재사용 여부를 판단한다.
    @Column(name = "query_hash", nullable = false, length = 64)
    private String queryHash;

    // 실제 검색 adapter가 사용한 embedding provider를 저장한다.
    @Column(name = "embedding_provider", nullable = false, length = 100)
    private String embeddingProvider;

    // 같은 provider 안의 벡터 규칙을 식별하는 model을 저장한다.
    @Column(name = "embedding_model", nullable = false, length = 150)
    private String embeddingModel;

    // 검색 당시 요청한 최대 결과 수를 재사용 계약에 고정한다.
    @Column(name = "top_k", nullable = false)
    private int topK;

    // 후보 유무에 따른 정상 종료 상태를 저장한다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RetrievalStatus status;

    // 검색 계약과 상태가 확정된 requirement retrieval 결과를 생성한다.
    public static RequirementRetrievalResult create(
            AnalysisInputSnapshot snapshot, Long userId, String requirementId, RequirementType requirementType,
            String requirementText, RetrievalCorpusType corpusType, String queryText, String queryHash,
            String embeddingProvider, String embeddingModel, int topK, RetrievalStatus status
    ) {
        RequirementRetrievalResult result = new RequirementRetrievalResult();
        result.snapshot = snapshot;
        result.userId = userId;
        result.requirementId = requirementId;
        result.requirementType = requirementType;
        result.requirementText = requirementText;
        result.corpusType = corpusType;
        result.queryText = queryText;
        result.queryHash = queryHash;
        result.embeddingProvider = embeddingProvider;
        result.embeddingModel = embeddingModel;
        result.topK = topK;
        result.status = status;
        return result;
    }
}
