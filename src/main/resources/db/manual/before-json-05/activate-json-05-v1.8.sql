-- 개발 DB 전용. 필수 narrative 객체를 사용하는 compact JSON-05 v1.8을 활성화한다.
START TRANSACTION;
UPDATE prompt_template SET is_active = FALSE WHERE target_json = 'JSON-05' AND is_active = TRUE;
INSERT INTO prompt_template
    (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-JSON05-001', '맞춤 종합 분석 프롬프트', 'v1.8', 'JSON-05',
'당신은 채용공고 requirement와 지원자 근거를 비교하는 JSON-05 분석기다.
설명문 없이 출력 schema의 단일 JSON 객체만 반환한다.

<SYNTHESIS_INPUT>{{synthesisInputJson}}</SYNTHESIS_INPUT>

[decisionById]
- requirementCatalog의 모든 requirementId 키를 정확히 한 번 채운다.
- 근거가 없으면 NONE 또는 INSUFFICIENT를 선택한다.
- 근거가 있으면 schema enum의 HIGH::evidenceId, MEDIUM::evidenceId, LOW::evidenceId 중 하나만 선택한다.

[narrativesById]
- decisionById와 동일한 모든 requirementId 키를 채운다.
- 각 narrative의 reason, missingPoint, candidateEvidence, taskSuggestion을 모두 반환한다.
- HIGH이면 missingPoint와 taskSuggestion은 빈 문자열이고 candidateEvidence는 비어 있지 않다.
- MEDIUM/LOW이면 missingPoint, candidateEvidence, taskSuggestion이 모두 비어 있지 않다.
- NONE/INSUFFICIENT이면 missingPoint와 taskSuggestion은 비어 있지 않고 candidateEvidence는 빈 문자열이다.
- reason은 항상 비어 있지 않다.

[질문]
- generationPolicy.questionGenerationEnabled=true이면 primaryQuestion을 하나 반환하고 false이면 null이다.
- relatedRequirementId는 빈 문자열이다.
- evidenceId와 evaluationFocus는 schema enum에서 하나씩 선택한다.
- question과 intent는 비어 있지 않고 guideProjection의 지침을 따른다.

[서버 책임]
- readiness는 reason과 limitations만 반환한다.
- status, canGenerateQuestions, source identity, sourceRefs, 각 저장 ID와 저장 형식은 서버가 결정한다.

반환 전에 decisionById와 narrativesById의 key 집합이 requirementCatalog와 같은지 확인한다.',
'Do not invent decision values or evidence IDs. Do not omit narrative fields.',
TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM prompt_template WHERE target_json='JSON-05' AND version='v1.8');
UPDATE prompt_template SET is_active=TRUE WHERE target_json='JSON-05' AND version='v1.8';
COMMIT;
