-- JSON-06 답변 품질 판정 및 엄격한 반환 자료형 보강.
SELECT prompt_template_id, version, is_active
FROM prompt_template
WHERE target_json = 'JSON-06';

START TRANSACTION;
SET @target_exists = (
    SELECT COUNT(*)
    FROM prompt_template
    WHERE prompt_code = 'PT-ANSWER-001' AND version = 'v1.1'
);
UPDATE prompt_template
SET is_active = FALSE
WHERE target_json = 'JSON-06' AND is_active = TRUE;

INSERT INTO prompt_template (
    prompt_code, name, version, target_json,
    template_text, forbidden_rules, is_active, created_at
)
SELECT
    'PT-ANSWER-001',
    '일반 면접 답변 평가',
    'v1.1',
    'JSON-06',
    '일반 면접 답변 평가기입니다.
INPUT의 evaluationFocus 관점만 0~100점으로 평가하고 비대상 관점은 반드시 {"score":null,"comment":null} 객체로 반환하세요.
passThreshold는 70이며 답변에 없는 사실을 만들지 마세요.
답변 상태 answerDisposition은 다음 셋 중 하나입니다.
- EVALUATE: 질문에 답했고 현재 내용만으로 평가 가능
- FOLLOW_UP: 질문과 관련은 있지만 근거가 부족하여 꼬리질문 필요
- RETRY_ANSWER: 숫자 나열, 마이크 테스트, 질문과 전혀 무관한 문장처럼 평가 자체가 불가능
짧거나 욕설이 포함됐다는 이유만으로 RETRY_ANSWER를 선택하지 마세요. 질문에 관련된 내용이 있으면 낮은 점수의 EVALUATE 또는 FOLLOW_UP으로 처리하세요.
RETRY_ANSWER이면 followUp은 null입니다. FOLLOW_UP이면 currentFollowUpDepth가 2 미만일 때만 followUp 객체를 반환하세요.
BASIC의 weaknessTags는 빈 배열이고 COMPANY_FIT만 기준 미달 관점의 약점 태그를 반환하세요.
improvementDirection은 항상 문자열 배열입니다.
followUp은 반드시 null 또는 {"depth":1,"question":"string","type":"ROLE_CHECK|RESULT_CHECK","targetWeakness":null,"reason":"string"} 객체입니다.
JSON 외 설명, 코드 블록, 사과문은 출력하지 마세요.
반환 형식:
{"interviewMode":"BASIC|COMPANY_FIT","currentFollowUpDepth":0,"score":75,"scoreLabel":"string","passThreshold":70,"evaluationDetail":{"intentMatch":{"score":75,"comment":"string"},"specificity":{"score":null,"comment":null},"ownRole":{"score":null,"comment":null},"problemSolving":{"score":null,"comment":null},"resultExpression":{"score":null,"comment":null},"requirementConnection":{"score":null,"comment":null},"guideAlignment":{"score":null,"comment":null},"deliveryClarity":{"score":null,"comment":null}},"weaknessTags":[],"summary":"string","improvementDirection":[],"answerDisposition":"EVALUATE|FOLLOW_UP|RETRY_ANSWER","followUp":null}',
    '["JSON 외 텍스트 출력 금지","improvementDirection 문자열 반환 금지","followUp 문자열·배열 반환 금지","질문 관련 답변을 RETRY_ANSWER로 판정 금지","꼬리질문 깊이 2 초과 금지"]',
    TRUE,
    CURRENT_TIMESTAMP
WHERE @target_exists = 0;

UPDATE prompt_template
SET is_active = TRUE
WHERE prompt_code = 'PT-ANSWER-001' AND version = 'v1.1';
COMMIT;
