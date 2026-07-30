-- 목적: v1.0은 JSON 스키마를 정의하지 않아 Claude가 마크다운 리포트로 응답하는 문제가 있었다.
-- JSON_SHAPE를 명시하고 코드블록/설명 금지를 명확히 한 v1.1을 활성화한다.
SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE target_json = 'JSON-07'
ORDER BY prompt_template_id;

START TRANSACTION;

UPDATE prompt_template
SET is_active = FALSE
WHERE target_json = 'JSON-07' AND is_active = TRUE;

INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-REPORT-001', '최종 면접 리포트', 'v1.1', 'JSON-07',
'완료된 면접 세션의 답변 평가 결과를 종합해 최종 리포트를 아래 JSON 형식으로만 작성한다.
설명문, Markdown(제목·표·이모지·굵은 글씨 등), 코드블록(```) 없이, 이 구조를 그대로 따르는 순수 JSON 객체 하나로만 응답한다.

입력에는 세션의 총질문·제출·평가성공·평가실패·미답변 개수, 서버가 계산한 overallScore·categoryScores, 그리고 평가 성공한 답변별 score·weaknessTags·summary가 주어진다.
직무분류·경력수준·가이드·요구사항 연결·근거 부족 정보가 입력에 없으면 해당 필드는 null 또는 빈 배열로 반환한다. 입력에 없는 사실을 지어내지 않는다.

{
  "overallScore": 0~100 사이 정수 (입력의 overallScore를 그대로 반영, 없으면 평가 성공 답변들의 평균),
  "scoreLabel": "세션 종합 기준 충족도",
  "categoryScores": {
    "intentMatch": 0~100 정수 또는 null,
    "specificity": 0~100 정수 또는 null,
    "ownRole": 0~100 정수 또는 null,
    "problemSolving": 0~100 정수 또는 null,
    "resultExpression": 0~100 정수 또는 null,
    "requirementConnection": 0~100 정수 또는 null (COMPANY_FIT 모드가 아니면 null),
    "guideAlignment": 0~100 정수 또는 null (적용 가이드가 없으면 null),
    "deliveryClarity": 0~100 정수 또는 null
  },
  "basisSummary": {
    "jobCategory": "입력에 있으면 직무 대분류/중분류 문자열, 없으면 null",
    "careerLevel": "NEW" 또는 "EXPERIENCED" 또는 "ANY" 또는 null,
    "evaluationPassThreshold": 정수 (입력에 있는 통과 기준 점수, 없으면 null),
    "usedGuide": {"guideId": 정수 또는 null, "version": "문자열 또는 null"},
    "requirementConnections": [{"requirement": "요구사항 문자열", "matchLevel": "HIGH"|"MEDIUM"|"LOW"|"NONE"|"INSUFFICIENT"}],
    "missingEvidence": ["부족한 근거 문자열", ...],
    "targetWeaknessTag": null,
    "targetDimension": null,
    "originEvaluationIds": []
  },
  "weaknessTagSummary": [
    {"tag": "입력 answer별 weaknessTags에 실제로 등장한 태그 문자열", "count": 그 태그가 등장한 답변 개수}
  ],
  "nextPracticeRecommendation": [
    {"questionType": "GENERAL"|"COMPANY_FIT"|"EXPERIENCE"|"PROBLEM_SOLVING"|"SKILL", "reason": "추천 이유 문자열"}
  ],
  "improvementSuggestion": {
    "resume": ["보완 제안 문자열", ...],
    "coverLetter": ["보완 제안 문자열", ...],
    "portfolio": ["보완 제안 문자열", ...],
    "experienceNote": ["보완 제안 문자열", ...]
  },
  "learningDirection": ["학습 방향 문자열", ...]
}

BASIC 모드에서는 requirementConnection을 null로, weaknessTagSummary와 improvementSuggestion의 각 배열은 빈 배열로 채운다.
weaknessTagSummary는 입력 answer들의 weaknessTags를 집계한 결과만 반환하고, 입력에 없는 태그를 새로 만들지 않는다.'
,
'["JSON 외 텍스트·Markdown·코드블록 출력 금지","입력에 없는 점수·태그·근거 생성 금지","미답변 질문 평가 금지"]',
TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template WHERE target_json = 'JSON-07' AND version = 'v1.1'
);

UPDATE prompt_template SET is_active = TRUE WHERE target_json = 'JSON-07' AND version = 'v1.1';

SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE target_json = 'JSON-07'
ORDER BY prompt_template_id;

COMMIT;