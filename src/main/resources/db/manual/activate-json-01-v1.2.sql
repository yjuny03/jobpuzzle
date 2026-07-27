-- 목적: v1.1을 보존하고 JSON-01 evidenceText 복사 절차를 강화한 v1.2를 개발 DB에서 활성화한다.
SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE prompt_code = 'PT-JOB-001'
ORDER BY prompt_template_id;
SELECT COUNT(*) AS target_version_count
FROM prompt_template
WHERE prompt_code = 'PT-JOB-001' AND version = 'v1.2';

START TRANSACTION;

UPDATE prompt_template
SET is_active = FALSE
WHERE target_json = 'JSON-01' AND is_active = TRUE;

-- v1.1 정본의 공통 계약은 유지하고 source reference 규칙과 JSON 예시만 v1.2 정본으로 교체한다.
INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT
    prompt_code,
    name,
    'v1.2',
    target_json,
    REPLACE(
        REPLACE(template_text,
            'evidenceText는 선택한 marker 바로 뒤 segment에서 연속된 원문 문자열을 그대로 복사한다. 요약, 재서술, 번역, 맞춤법 보정, 공백·줄바꿈 정규화, 여러 구간 결합을 절대 하지 않는다.
evidenceText는 비어 있지 않아야 하며, 출력 전에 선택한 segment 안에 evidenceText가 문자 단위로 그대로 존재하는지 확인한다. 확신할 수 없으면 해당 sourceRefs를 만들지 말고 missingEvidence에 부족 사유를 기록한다.',
            'evidenceText는 선택한 marker 바로 뒤 segment에서 복사한 20~160자의 연속 원문 문자열만 사용한다. evidenceText를 새로 작성하지 말고, marker segment에서 먼저 복사한 뒤 그 복사본만 출력한다.
요약, 재서술, 번역, 맞춤법 보정, 공백·줄바꿈 정규화, 여러 구간 결합, 조사 변경을 절대 하지 않는다. text 필드에는 분석·요약을 써도 되지만 evidenceText에는 절대 적용하지 않는다.
각 sourceRefs를 출력하기 직전에 선택한 segment 안에 evidenceText가 문자 단위로 그대로 존재하는지 확인한다. 확인할 수 없으면 sourceRefs를 만들지 말고 해당 항목을 생략하거나 missingEvidence에 부족 사유를 기록한다.'),
        '선택한 marker segment에서 그대로 복사한 연속 원문',
        'marker segment에서 복사한 20~160자 연속 원문'),
    REPLACE(forbidden_rules,
        'evidenceText를 요약·재서술·정규화하지 않는다.',
        'evidenceText를 새로 작성·요약·재서술·정규화하지 않는다.'),
    TRUE,
    CURRENT_TIMESTAMP
FROM prompt_template
WHERE prompt_code = 'PT-JOB-001' AND version = 'v1.1';

SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE prompt_code = 'PT-JOB-001'
ORDER BY prompt_template_id;
COMMIT;
