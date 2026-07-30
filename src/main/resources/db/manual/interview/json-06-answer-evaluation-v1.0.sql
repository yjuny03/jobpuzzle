-- JSON-06 일반 답변 평가 프롬프트 수동 등록. 적용 전 동일 버전과 활성본을 확인한다.
SELECT prompt_template_id, version, is_active FROM prompt_template WHERE target_json = 'JSON-06';
START TRANSACTION;
SET @target_exists = (SELECT COUNT(*) FROM prompt_template WHERE prompt_code='PT-ANSWER-001' AND version='v1.0');
UPDATE prompt_template SET is_active = FALSE WHERE target_json = 'JSON-06' AND is_active = TRUE AND @target_exists = 0;
INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-ANSWER-001', '일반 면접 답변 평가', 'v1.0', 'JSON-06',
       '일반 면접 답변 평가기입니다. INPUT의 evaluationFocus 관점만 0~100점으로 평가하고 비대상 관점은 score=null로 반환하세요. passThreshold는 70이며 답변에 없는 사실을 만들지 마세요. BASIC의 weaknessTags는 빈 배열이고 COMPANY_FIT만 기준 미달 관점의 약점 태그를 반환하세요. currentFollowUpDepth가 2이거나 답변이 충분하면 followUp은 null입니다. JSON 외 설명은 출력하지 마세요. 반환 형식: {"interviewMode":"BASIC|COMPANY_FIT","currentFollowUpDepth":0,"score":75,"scoreLabel":"string","passThreshold":70,"evaluationDetail":{"intentMatch":{"score":75,"comment":"string"},"specificity":{"score":null,"comment":null},"ownRole":{"score":null,"comment":null},"problemSolving":{"score":null,"comment":null},"resultExpression":{"score":null,"comment":null},"requirementConnection":{"score":null,"comment":null},"guideAlignment":{"score":null,"comment":null},"deliveryClarity":{"score":null,"comment":null}},"weaknessTags":[],"summary":"string","improvementDirection":[],"followUp":null}',
       '["비대상 관점 채점 금지","근거 없는 사실 생성 금지","꼬리질문 깊이 2 초과 금지"]', TRUE, CURRENT_TIMESTAMP
WHERE @target_exists = 0;
COMMIT;
