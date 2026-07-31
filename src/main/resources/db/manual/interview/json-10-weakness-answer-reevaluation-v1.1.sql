-- JSON-10 약점 답변 재평가 계약 보강: 미통과 followUp 자료형을 명시한다.
START TRANSACTION;
SET @target_exists = (
    SELECT COUNT(*)
    FROM prompt_template
    WHERE prompt_code = 'PT-WEAK-A-001' AND version = 'v1.1'
);
UPDATE prompt_template
SET is_active = FALSE
WHERE target_json = 'JSON-10'
  AND is_active = TRUE
  AND @target_exists = 0;
INSERT INTO prompt_template (
    prompt_code, name, version, target_json, template_text,
    forbidden_rules, is_active, created_at
)
SELECT
    'PT-WEAK-A-001',
    '약점 답변 재평가',
    'v1.1',
    'JSON-10',
    '약점 답변 재평가기입니다.
INPUT의 targetWeaknessTag와 targetDimension을 문자열 그대로 반환하고 해당 단일 관점만 0~100점으로 평가하세요.
passThreshold는 항상 숫자 70이며 passed는 score >= 70과 일치하는 boolean입니다.
답변이 질문과 관련은 있으나 부족하면 낮은 점수로 평가하고, currentFollowUpDepth가 2 미만일 때만 followUp 질문으로 보완하세요.
currentFollowUpDepth가 2이거나 score >= 70이면 followUp은 반드시 null입니다.
followUp은 배열이나 문자열로 반환하지 말고, 반드시 null 또는 다음 객체 형식으로 반환하세요:
{"depth":1,"question":"string","type":"IMPROVEMENT_PLAN","reason":"string"}
depth는 currentFollowUpDepth + 1이며 type은 IMPROVEMENT_PLAN입니다.
JSON 외 설명이나 Markdown 코드 블록을 출력하지 마세요.
반환 형식:
{"targetWeaknessTag":"string","targetDimension":"string","currentFollowUpDepth":0,"score":75,"passThreshold":70,"comment":"string","passed":true,"followUp":null}',
    '["다른 관점 평가 금지","근거 없는 개선 판정 금지","followUp 배열·문자열 반환 금지","정의되지 않은 followUp type 반환 금지"]',
    TRUE,
    CURRENT_TIMESTAMP
WHERE @target_exists = 0;
UPDATE prompt_template
SET is_active = (version = 'v1.1')
WHERE target_json = 'JSON-10'
  AND EXISTS (
      SELECT 1
      FROM (
          SELECT prompt_template_id
          FROM prompt_template
          WHERE prompt_code = 'PT-WEAK-A-001' AND version = 'v1.1'
      ) AS active_json10
  );
COMMIT;
