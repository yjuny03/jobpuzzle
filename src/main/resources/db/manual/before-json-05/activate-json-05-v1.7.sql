-- 개발 DB 전용. grammar 크기를 줄인 compact JSON-05 v1.7을 활성화한다.
START TRANSACTION;
UPDATE prompt_template SET is_active = FALSE
WHERE target_json = 'JSON-05' AND is_active = TRUE;
INSERT INTO prompt_template
    (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-JSON05-001', '맞춤 종합 분석 프롬프트', 'v1.7', 'JSON-05',
'당신은 채용공고 requirement와 지원자 근거를 비교하는 JSON-05 분석기다.
설명문 없이 출력 schema의 단일 JSON 객체만 반환한다.

<SYNTHESIS_INPUT>{{synthesisInputJson}}</SYNTHESIS_INPUT>

[decisionById]
- requirementCatalog의 모든 requirementId 키를 정확히 한 번 채운다.
- 지원자 근거가 없으면 NONE 또는 INSUFFICIENT를 선택한다.
- 지원자 근거가 있으면 schema가 허용한 HIGH::evidenceId, MEDIUM::evidenceId, LOW::evidenceId 중 하나만 선택한다.
- decision 문자열을 직접 만들지 말고 schema enum에서 고른다.

[narrativesById]
- decisionById와 동일한 모든 requirementId 키를 채운다.
- 각 값은 반드시 다음 순서의 문자열 4개다.
  1. reason
  2. missingPoint
  3. candidateEvidence
  4. taskSuggestion
- HIGH이면 missingPoint와 taskSuggestion은 빈 문자열이고 candidateEvidence는 비어 있지 않다.
- MEDIUM/LOW이면 missingPoint, candidateEvidence, taskSuggestion이 모두 비어 있지 않다.
- NONE/INSUFFICIENT이면 missingPoint와 taskSuggestion은 비어 있지 않고 candidateEvidence는 빈 문자열이다.
- reason은 항상 비어 있지 않다.

[질문]
- generationPolicy.questionGenerationEnabled=true이면 primaryQuestion을 하나 반환하고 false이면 null이다.
- relatedRequirementId는 빈 문자열이다.
- evidenceId는 evidenceCatalog에 있는 ID 하나, evaluationFocus는 schema enum 하나를 선택한다.
- question과 intent는 비어 있지 않으며 guideProjection의 지침을 따른다.

[서버 책임]
- readiness는 reason과 limitations만 반환한다.
- status, canGenerateQuestions, source identity, sourceRefs, match/task/question ID와 저장 형식은 서버가 결정한다.

반환 전에 decisionById와 narrativesById의 key 집합이 requirementCatalog와 같은지, 각 narrative가 정확히 4개인지 확인한다.',
'Do not invent decision values or evidence IDs. Do not return server-owned source identity fields.',
TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template WHERE target_json = 'JSON-05' AND version = 'v1.7'
);
UPDATE prompt_template SET is_active = TRUE
WHERE target_json = 'JSON-05' AND version = 'v1.7';
COMMIT;
