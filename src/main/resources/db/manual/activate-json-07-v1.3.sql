-- 목적: v1.2는 overallScore/overallAssessment 등 숫자·총평은 갖췄지만
-- (1) learningDirection·nextPracticeRecommendation.reason의 서술 상세도가 낮고,
-- (2) 제출하지 않은 자료(coverLetter/portfolio/experienceNote 등)에 대해서도
--     improvementSuggestion을 만들어내는 문제가 있었다.
-- (3) nextPracticeRecommendation.questionType enum이 InterviewQuestionType의 11개 값 중 5개만 나열되어 있었다.
-- 세 가지를 보강한 v1.3을 활성화한다.

SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE target_json = 'JSON-07'
ORDER BY prompt_template_id;

START TRANSACTION;

UPDATE prompt_template
SET is_active = FALSE
WHERE target_json = 'JSON-07' AND is_active = TRUE;

INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-REPORT-001', '최종 면접 리포트', 'v1.3', 'JSON-07',
'완료된 면접 세션의 답변 평가 결과를 종합해 최종 리포트를 아래 JSON 형식으로만 작성한다.
설명문, Markdown(제목·표·이모지·굵은 글씨 등), 코드블록(```) 없이, 이 구조를 그대로 따르는 순수 JSON 객체 하나로만 응답한다.

입력에는 세션의 총질문·제출·평가성공·평가실패·미답변 개수, 서버가 계산한 overallScore·categoryScores,
평가 성공한 답변별 score·weaknessTags·summary, 그리고 이번 세션에서 실제로 제출된 자료 유형(제출자료) 목록이 주어진다.
직무분류·경력수준·가이드·요구사항 연결·근거 부족 정보가 입력에 없으면 해당 필드는 null 또는 빈 배열로 반환한다. 입력에 없는 사실을 지어내지 않는다.

{
  "overallScore": 0~100 사이 정수 (입력의 overallScore를 그대로 반영, 없으면 평가 성공 답변들의 평균),
  "scoreLabel": "세션 종합 기준 충족도",
  "overallAssessment": "이번 면접 전체를 종합한 총평 문단",
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
    {"questionType": "GENERAL"|"COMPANY_FIT"|"EXPERIENCE"|"PROBLEM_SOLVING"|"SKILL"|"SELF_INTRO"|"MOTIVATION"|"STRENGTH_WEAKNESS"|"FAILURE_CONFLICT"|"JOB_GENERAL"|"WEAKNESS_FOLLOWUP", "reason": "추천 이유 문자열"}
  ],
  "improvementSuggestion": {
    "resume": ["보완 제안 문자열", ...],
    "coverLetter": ["보완 제안 문자열", ...],
    "portfolio": ["보완 제안 문자열", ...],
    "experienceNote": ["보완 제안 문자열", ...]
  },
  "learningDirection": ["학습 방향 문자열", ...]
}

overallAssessment 작성 규칙:
- 입력으로 주어진 답변별 score·weaknessTags·summary와 세션 overallScore·categoryScores만 근거로 3~5문장의 한국어 문단 하나로 작성한다.
- 첫 문장은 이번 면접에서 전반적으로 어느 수준이었는지로 시작한다.
- 이어서 어느 부분(관점 또는 반복된 약점 태그)에서 특히 취약했는지를 구체적으로 짚는다.
- 마지막 문장은 종합했을 때 지금 상태가 어떤지에 대한 결론으로 마무리한다.
- 합격·불합격, 채용 가능성으로 해석될 수 있는 표현은 쓰지 않는다. 입력에 없는 사실·수치를 지어내지 않는다.

learningDirection 작성 규칙:
- 각 항목에 "무엇을 학습/연습해야 하는지"와 "그렇게 판단한 근거(어떤 weaknessTag 또는 summary에서 나온 것인지)"를 한 문장 안에 함께 담는다.

nextPracticeRecommendation 작성 규칙:
- reason에는 어떤 categoryScores 또는 weaknessTags 근거로 그 questionType을 추천하는지 구체적으로 명시한다.
- questionType은 위 JSON 예시에 나열된 11개 값 중 하나만 사용한다.

improvementSuggestion 작성 규칙 (중요):
- 입력의 제출자료 목록에 있는 문서 유형에 대해서만 배열을 채운다.
- 제출자료 목록에 없는 문서 유형(예: 이력서만 제출했다면 coverLetter·portfolio·experienceNote)은 반드시 빈 배열([])로 반환한다.
- 제출하지 않은 자료에 대한 보완 제안을 만들어내지 않는다.

BASIC 모드에서는 requirementConnection을 null로, weaknessTagSummary와 improvementSuggestion의 각 배열은 빈 배열로 채운다.
weaknessTagSummary는 입력 answer들의 weaknessTags를 집계한 결과만 반환하고, 입력에 없는 태그를 새로 만들지 않는다.'
,
'["JSON 외 텍스트·Markdown·코드블록 출력 금지","입력에 없는 점수·태그·근거 생성 금지","미답변 질문 평가 금지","합격 가능성·채용 여부로 해석되는 표현 금지","제출하지 않은 자료에 대한 보완 제안 생성 금지"]',
TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template WHERE target_json = 'JSON-07' AND version = 'v1.3'
);

UPDATE prompt_template SET is_active = TRUE WHERE target_json = 'JSON-07' AND version = 'v1.3';

SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE target_json = 'JSON-07'
ORDER BY prompt_template_id;

COMMIT;