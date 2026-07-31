-- JSON-09 약점 질문 생성 프롬프트 수동 등록.
SELECT prompt_template_id, version, is_active FROM prompt_template WHERE target_json = 'JSON-09';
START TRANSACTION;
SET @target_exists = (SELECT COUNT(*) FROM prompt_template WHERE prompt_code='PT-WEAK-Q-001' AND version='v1.0');
UPDATE prompt_template
SET template_text = '약점 보완 질문 생성기입니다. <WEAKNESS_INPUT>{{weaknessInputJson}}</WEAKNESS_INPUT>의 originEvaluations마다 질문 하나를 생성하고 originEvaluationId, targetWeaknessTag, targetDimension을 그대로 반환하세요. questionType은 WEAKNESS_FOLLOWUP입니다. 각 질문은 questionId, questionType, question, intent, evaluationFocus, originEvaluationId, targetWeaknessTag, targetDimension, reviewStatus, reviewNote를 포함하고 reviewStatus는 PASS여야 합니다. 최대 10개이며 {"questions":[...]} JSON 객체만 반환하세요.'
WHERE prompt_code='PT-WEAK-Q-001' AND version='v1.0';
UPDATE prompt_template SET is_active = FALSE WHERE target_json = 'JSON-09' AND is_active = TRUE AND @target_exists = 0;
INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-WEAK-Q-001', '약점 보완 질문 생성', 'v1.0', 'JSON-09',
       '약점 보완 질문 생성기입니다. <WEAKNESS_INPUT>{{weaknessInputJson}}</WEAKNESS_INPUT>의 originEvaluations마다 질문 하나를 생성하고 originEvaluationId, targetWeaknessTag, targetDimension을 그대로 반환하세요. questionType은 WEAKNESS_FOLLOWUP입니다. 각 질문은 questionId, questionType, question, intent, evaluationFocus, originEvaluationId, targetWeaknessTag, targetDimension, reviewStatus, reviewNote를 포함하고 reviewStatus는 PASS여야 합니다. 최대 10개이며 {"questions":[...]} JSON 객체만 반환하세요.',
       '["입력 평가 누락 금지","대상 관점 변경 금지"]', TRUE, CURRENT_TIMESTAMP
WHERE @target_exists = 0;
COMMIT;
