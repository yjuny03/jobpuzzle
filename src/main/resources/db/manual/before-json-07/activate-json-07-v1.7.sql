-- 목적: v1.6의 weaknessTagSummary.reason 등 서술형 텍스트에서 "score 10점 답변"처럼
-- 영문 단어 score가 그대로 노출되는 문제가 있었다. 어떤 점수인지 한글로 명확히 쓰도록
-- 한글 표기 규칙을 확장한 v1.7을 활성화한다.

SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE target_json = 'JSON-07'
ORDER BY prompt_template_id;

START TRANSACTION;

UPDATE prompt_template
SET is_active = FALSE
WHERE target_json = 'JSON-07' AND is_active = TRUE;

INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-REPORT-001', '최종 면접 리포트', 'v1.7', 'JSON-07',
'완료된 면접 세션의 답변 평가 결과를 종합해 최종 리포트를 아래 JSON 형식으로만 작성한다.
설명문, Markdown(제목·표·이모지·굵은 글씨 등), 코드블록(```) 없이, 이 구조를 그대로 따르는 순수 JSON 객체 하나로만 응답한다.

입력에는 세션의 총질문·제출·평가성공·평가실패·미답변 개수, 서버가 계산한 overallScore·categoryScores,
평가 성공한 답변별 score·weaknessTags·summary, 그리고 이번 세션에서 실제로 제출된 자료 유형(제출자료) 목록이 주어진다.
직무분류·경력수준·가이드·요구사항 연결·근거 부족 정보가 입력에 없으면 해당 필드는 null 또는 빈 배열로 반환한다. 입력에 없는 사실을 지어내지 않는다.

{
  "overallScore": 0~100 사이 정수 (입력의 overallScore를 그대로 반영, 없으면 평가 성공 답변들의 평균),
  "scoreLabel": "세션 종합 기준 충족도",
  "overallAssessment": "[잘한 점]/[부족한 점]/[총평] 3개 섹션으로 구성된 총평 (작성 규칙 참고)",
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
    {"tag": "입력 answer별 weaknessTags에 실제로 등장한 태그 문자열", "count": 그 태그가 등장한 답변 개수,
     "reason": "그 태그가 왜 나왔는지에 대한 설명"}
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

[한글 표기 규칙 - 중요]
overallAssessment, learningDirection, nextPracticeRecommendation.reason, weaknessTagSummary.reason 등
사람이 읽는 서술형 텍스트에는 categoryScores·questionType의 영문 필드명·enum 값을 그대로 쓰지 말고,
반드시 아래 한글 표기로 바꿔서 쓴다.
categoryScores 한글 표기: intentMatch=질문 의도 이해, specificity=경험 구체성, ownRole=본인 역할,
problemSolving=문제 해결 과정, resultExpression=성과 및 결과 표현, requirementConnection=공고 요구사항 연결,
guideAlignment=직무 가이드 적합, deliveryClarity=답변 전달력.
questionType 한글 표기: GENERAL=일반, COMPANY_FIT=회사 맞춤, EXPERIENCE=경험, PROBLEM_SOLVING=문제 해결,
SKILL=기술, SELF_INTRO=자기소개, MOTIVATION=지원 동기, STRENGTH_WEAKNESS=강점·약점, FAILURE_CONFLICT=실패·갈등 경험,
JOB_GENERAL=직무 일반, WEAKNESS_FOLLOWUP=약점 보완 후속.
같은 서술형 텍스트에서 어떤 답변의 점수를 언급할 때는 "score"라는 영문 단어를 그대로 쓰지 말고,
그 점수가 무엇의 점수인지 드러나게 "답변 점수 N점"처럼 한글로 쓴다. 특정 관점 하나만의 점수를 말하는 것이면
그 관점의 한글 표기를 붙여 "구체성 점수 N점"처럼 쓴다. 그 외에도 sentence·score·tag처럼 JSON 필드명이 아닌
일반 영단어도 서술형 텍스트 안에서는 그대로 쓰지 않는다.
(JSON의 categoryScores 키 이름과 questionType 값 자체는 위 JSON 예시대로 영문 그대로 반환한다. 한글로 바꾸는 대상은
overallAssessment·learningDirection·reason 같은 서술형 문장 안의 표현일 뿐이다.)

overallAssessment 작성 규칙 (중요 - 3개 섹션 구조):
overallAssessment는 하나의 문자열이며, 아래 순서로 3개 섹션을 줄바꿈으로 구분해서 채운다.
각 섹션 제목은 대괄호로 감싸서 그대로 쓴다: [잘한 점], [부족한 점], [총평]

[잘한 점]
- 입력 answer들 중 점수가 높았던 관점이나 summary에서 확인된 강점을 근거로 서술한다.
- 강점이 1개뿐이면 그 1개만 짧게 서술하고, 없는 내용을 지어내 억지로 문장을 늘리지 않는다.
- 강점이 여러 개면 각각을 간결하게 서술한다.

[부족한 점] (세션 종합 점수: N점)
- 섹션 제목 바로 뒤 괄호의 N 자리에 입력으로 주어진 세션 overallScore 값을 그대로 정수로 넣는다.
- 이어서 이번 면접 세션 전체에서 부족했던 부분을 1~3문장의 서술형으로 쓴다. 어떤 관점(categoryScores)이나
  반복된 weaknessTags 경향을 근거로, 무엇이 부족했는지 구체적으로 짚는다. [잘한 점]과 같은 문단 형식이며
  태그를 나열하는 목록이 아니다 (태그별 설명은 이 섹션이 아니라 weaknessTagSummary[].reason에 쓴다).
- 부족한 점이 뚜렷하게 1가지뿐이면 그 1개만 짧게 서술하고, 없는 내용을 지어내 억지로 문장을 늘리지 않는다.
- 부족한 점이 딱히 없으면 "특별히 부족한 부분은 확인되지 않았습니다." 한 줄만 쓴다.

[총평]
- 위 [잘한 점]과 [부족한 점]을 종합했을 때 지금 상태가 어떤지 1~2문장으로 결론짓는다.
- 합격·불합격, 채용 가능성으로 해석될 수 있는 표현은 쓰지 않는다.

세 섹션 모두 입력에 없는 사실·수치를 지어내지 않고, 내용이 짧으면 짧은 대로 두고 불필요하게 문장을 늘리지 않는다.

weaknessTagSummary 작성 규칙:
- 입력 answer들의 weaknessTags를 태그별로 집계해 tag·count를 채운다. 입력에 없는 태그를 새로 만들지 않는다.
- reason에는 그 태그가 어떤 답변의 점수·근거로 나오게 됐는지 구체적으로 설명한다. 점수를 언급할 때는
  위 한글 표기 규칙대로 "답변 점수 N점" 또는 "관점명 점수 N점" 형식으로 쓴다.
- 같은 태그가 여러 답변에서 반복됐으면 그 근거들을 종합해 한 문장으로 쓴다.

learningDirection 작성 규칙:
- 각 항목에 "무엇을 학습/연습해야 하는지"와 "그렇게 판단한 근거(어떤 weaknessTag 또는 summary에서 나온 것인지)"를 한 문장 안에 함께 담는다.

nextPracticeRecommendation 작성 규칙:
- reason에는 어떤 categoryScores 또는 weaknessTags 근거로 그 questionType을 추천하는지 구체적으로 명시한다.
- questionType 필드 자체는 위 JSON 예시에 나열된 11개 영문 값 중 하나만 사용하고, reason 문장 안에서는 위 한글 표기를 사용한다.

improvementSuggestion 작성 규칙 (중요):
- 입력의 제출자료 목록에 있는 문서 유형에 대해서만 배열을 채운다.
- 제출자료 목록에 없는 문서 유형(예: 이력서만 제출했다면 coverLetter·portfolio·experienceNote)은 반드시 빈 배열([])로 반환한다.
- 제출하지 않은 자료에 대한 보완 제안을 만들어내지 않는다.

BASIC 모드에서는 requirementConnection을 null로, weaknessTagSummary와 improvementSuggestion의 각 배열은 빈 배열로 채운다.'
,
'["JSON 외 텍스트·Markdown·코드블록 출력 금지","입력에 없는 점수·태그·근거 생성 금지","미답변 질문 평가 금지","합격 가능성·채용 여부로 해석되는 표현 금지","제출하지 않은 자료에 대한 보완 제안 생성 금지","서술형 텍스트에 영문 필드명·enum 값·score 같은 영단어 그대로 노출 금지","overallAssessment를 [잘한 점]/[부족한 점]/[총평] 3개 섹션 형식 외로 작성 금지","weaknessTagSummary에 reason 없이 tag·count만 반환 금지"]',
TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template WHERE target_json = 'JSON-07' AND version = 'v1.7'
);

UPDATE prompt_template SET is_active = TRUE WHERE target_json = 'JSON-07' AND version = 'v1.7';

SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE target_json = 'JSON-07'
ORDER BY prompt_template_id;

COMMIT;