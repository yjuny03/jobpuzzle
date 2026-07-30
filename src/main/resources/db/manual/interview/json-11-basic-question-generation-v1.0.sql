-- JSON-11 기본 질문 생성 프롬프트 수동 등록.
SELECT prompt_template_id, version, is_active FROM prompt_template WHERE target_json = 'JSON-11';
START TRANSACTION;
SET @target_exists = (SELECT COUNT(*) FROM prompt_template WHERE prompt_code='PT-BASIC-Q-001' AND version='v1.0');
UPDATE prompt_template
SET template_text = '직무 면접 질문 생성기입니다. <BASIC_INPUT>{{basicInputJson}}</BASIC_INPUT>에 맞춰 SELF_INTRO, MOTIVATION, STRENGTH_WEAKNESS, FAILURE_CONFLICT, JOB_GENERAL을 순서대로 정확히 한 번씩 생성하세요. 각 질문은 questionId, questionType, question, intent, evaluationFocus, originEvaluationId, targetWeaknessTag, targetDimension, reviewStatus, reviewNote를 포함해야 합니다. originEvaluationId, targetWeaknessTag, targetDimension은 null이고 reviewStatus는 PASS여야 합니다. BASIC에는 requirementConnection을 사용하지 말고 {"questions":[...]} JSON 객체만 반환하세요.'
WHERE prompt_code='PT-BASIC-Q-001' AND version='v1.0';
UPDATE prompt_template SET is_active = FALSE WHERE target_json = 'JSON-11' AND is_active = TRUE AND @target_exists = 0;
INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-BASIC-Q-001', '기본 면접 질문 생성', 'v1.0', 'JSON-11',
       '직무 면접 질문 생성기입니다. <BASIC_INPUT>{{basicInputJson}}</BASIC_INPUT>에 맞춰 SELF_INTRO, MOTIVATION, STRENGTH_WEAKNESS, FAILURE_CONFLICT, JOB_GENERAL을 순서대로 정확히 한 번씩 생성하세요. 각 질문은 questionId, questionType, question, intent, evaluationFocus, originEvaluationId, targetWeaknessTag, targetDimension, reviewStatus, reviewNote를 포함해야 합니다. originEvaluationId, targetWeaknessTag, targetDimension은 null이고 reviewStatus는 PASS여야 합니다. BASIC에는 requirementConnection을 사용하지 말고 {"questions":[...]} JSON 객체만 반환하세요.',
       '["질문 유형 누락·중복 금지","requirementConnection 사용 금지"]', TRUE, CURRENT_TIMESTAMP
WHERE @target_exists = 0;
COMMIT;
