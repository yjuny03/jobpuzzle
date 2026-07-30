-- JSON-10 약점 답변 재평가 프롬프트 수동 등록.
SELECT prompt_template_id, version, is_active FROM prompt_template WHERE target_json = 'JSON-10';
START TRANSACTION;
SET @target_exists = (SELECT COUNT(*) FROM prompt_template WHERE prompt_code='PT-WEAK-A-001' AND version='v1.0');
UPDATE prompt_template SET is_active = FALSE WHERE target_json = 'JSON-10' AND is_active = TRUE AND @target_exists = 0;
INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-WEAK-A-001', '약점 답변 재평가', 'v1.0', 'JSON-10',
       '약점 답변 재평가기입니다. INPUT의 targetWeaknessTag와 targetDimension을 그대로 사용해 단일 관점만 0~100점으로 평가하세요. passThreshold는 70이고 passed는 score >= 70과 일치해야 합니다. currentFollowUpDepth가 2이거나 통과하면 followUp은 null입니다. JSON 외 설명은 출력하지 마세요. 반환 형식: {"targetWeaknessTag":"string","targetDimension":"string","currentFollowUpDepth":0,"score":75,"passThreshold":70,"comment":"string","passed":true,"followUp":null}',
       '["다른 관점 평가 금지","근거 없는 개선 판정 금지"]', TRUE, CURRENT_TIMESTAMP
WHERE @target_exists = 0;
COMMIT;
