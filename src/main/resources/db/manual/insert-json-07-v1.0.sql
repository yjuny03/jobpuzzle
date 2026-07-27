-- JSON-07 최종 리포트 프롬프트 수동 등록.
SELECT prompt_template_id, version, is_active FROM prompt_template WHERE target_json = 'JSON-07';
START TRANSACTION;
SET @target_exists = (SELECT COUNT(*) FROM prompt_template WHERE prompt_code='PT-REPORT-001' AND version='v1.0');
UPDATE prompt_template SET is_active = FALSE WHERE target_json = 'JSON-07' AND is_active = TRUE AND @target_exists = 0;
INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-REPORT-001', '최종 면접 리포트', 'v1.0', 'JSON-07',
       '완료된 면접 세션의 최종 리포트 생성기입니다. 답변하지 않은 질문과 실패 평가를 제외하고 입력의 확정 점수·평가·약점만 사용해 JSON-07 객체만 반환하세요.',
       '["입력에 없는 점수 생성 금지","미답변 질문 평가 금지"]', TRUE, CURRENT_TIMESTAMP
WHERE @target_exists = 0;
COMMIT;
