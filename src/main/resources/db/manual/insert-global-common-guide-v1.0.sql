-- 정확한 직무·경력 가이드가 없을 때 사용하는 최종 공통 fallback 가이드입니다.
-- 특정 직무 가이드를 대체하지 않으며 GuideContextService 조회 순서의 마지막에서만 선택됩니다.

START TRANSACTION;

INSERT INTO job_guide_document (
    guide_code, previous_guide_id, scope_type, job_category_id, scope_main_category,
    title, source_type, file_path, status, version, created_by, applicable_scope,
    evaluation_focus, evidence_rules, question_direction, avoid_questions,
    created_at, updated_at
)
SELECT
    'GUIDE-GLOBAL-COMMON-001', NULL, 'GLOBAL_COMMON', NULL, NULL,
    '전 직무 공통 면접 분석 가이드', 'DIRECT_INPUT', NULL, 'ACTIVE', 'v1.0',
    (SELECT MIN(user_id) FROM user),
    '정확한 세부 직무·경력 가이드가 등록되지 않은 모든 직무',
    JSON_ARRAY('questionIntent', 'answerClarity', 'evidenceSpecificity', 'ownRole', 'resultExpression', 'problemSolving'),
    JSON_ARRAY(
        '지원자가 제공한 자료에 존재하는 근거만 사용한다.',
        '팀 성과와 지원자 본인의 역할을 구분한다.',
        '주장에는 행동, 판단 근거 또는 결과를 연결한다.'
    ),
    JSON_ARRAY(
        '공고의 요구사항과 지원자 경험의 연결 근거를 확인한다.',
        '지원자의 실제 역할과 의사결정 과정을 구체화한다.',
        '성과와 배운 점을 직무 수행 역량에 연결한다.'
    ),
    JSON_ARRAY(
        '자료에 없는 경력이나 성과를 전제로 질문하지 않는다.',
        '하나의 질문에서 서로 다른 경험을 과도하게 요구하지 않는다.'
    ),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM job_guide_document
    WHERE guide_code = 'GUIDE-GLOBAL-COMMON-001' AND version = 'v1.0'
)
AND EXISTS (SELECT 1 FROM user);

SET @global_common_guide_id = (
    SELECT guide_id
    FROM job_guide_document
    WHERE guide_code = 'GUIDE-GLOBAL-COMMON-001' AND version = 'v1.0'
    LIMIT 1
);

INSERT INTO job_guide_chunk (
    guide_id, chunk_index, title, content, content_summary, embedding_ref, created_at
)
SELECT @global_common_guide_id, 0, '공통 답변 평가 기준',
       '좋은 면접 답변은 질문의 의도를 이해하고, 지원자가 직접 수행한 행동과 판단 근거를 구체적으로 설명한다. 결과는 가능한 범위에서 수치나 관찰 가능한 변화로 표현하며, 해당 경험에서 배운 점을 지원 직무와 연결한다.',
       '질문 의도, 구체적 행동, 본인 역할, 결과와 직무 연결을 확인하는 공통 기준',
       NULL, CURRENT_TIMESTAMP
WHERE @global_common_guide_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM job_guide_chunk
      WHERE guide_id = @global_common_guide_id AND chunk_index = 0
  );

INSERT INTO job_guide_chunk (
    guide_id, chunk_index, title, content, content_summary, embedding_ref, created_at
)
SELECT @global_common_guide_id, 1, '공통 질문 생성 기준',
       '질문은 공고의 핵심 요구사항과 지원자 자료에서 확인된 경험을 연결해 생성한다. 근거가 충분한 영역은 역할과 성과를 깊게 확인하고, 근거가 부족한 영역은 경험을 꾸며내도록 유도하지 않고 보완 계획과 학습 경험을 확인한다.',
       '공고 요구사항과 지원자 근거 수준에 맞춘 공통 질문 생성 기준',
       NULL, CURRENT_TIMESTAMP
WHERE @global_common_guide_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM job_guide_chunk
      WHERE guide_id = @global_common_guide_id AND chunk_index = 1
  );

COMMIT;

SELECT guide_id, guide_code, scope_type, title, status, version
FROM job_guide_document
WHERE guide_code = 'GUIDE-GLOBAL-COMMON-001';
