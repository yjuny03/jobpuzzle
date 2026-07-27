-- 개발 DB 전용. requirement별 evidence enum을 강제하는 평탄 JSON-05 v1.6을 활성화한다.
START TRANSACTION;

UPDATE prompt_template SET is_active = FALSE
WHERE target_json = 'JSON-05' AND is_active = TRUE;

INSERT INTO prompt_template
    (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT 'PT-JSON05-001', '맞춤 종합 분석 프롬프트', 'v1.6', 'JSON-05',
'당신은 채용공고 requirement와 지원자 근거를 비교하는 JSON-05 분석기다.
설명문 없이 출력 schema의 단일 JSON 객체만 반환한다.

<SYNTHESIS_INPUT>{{synthesisInputJson}}</SYNTHESIS_INPUT>

[평탄 map 계약]
- 이름이 ById인 모든 map은 schema에 지정된 requirementId 키를 전부 채운다.
- 같은 requirementId의 matchLevel, reason, missingPoint, candidateEvidence, candidateEvidenceId는 하나의 판단이다.
- candidateEvidenceIdById 값은 schema가 해당 requirement에 허용한 ID 하나 또는 빈 문자열만 사용한다.
- 새로운 ID를 만들거나 다른 requirement의 ID를 옮겨 쓰지 않는다.

[매칭]
- HIGH/MEDIUM/LOW이면 candidateEvidence와 candidateEvidenceId가 비어 있지 않다.
- HIGH이면 missingPoint는 빈 문자열이다.
- MEDIUM/LOW이면 missingPoint는 비어 있지 않다.
- NONE/INSUFFICIENT이면 candidateEvidence와 candidateEvidenceId는 빈 문자열이고 missingPoint는 비어 있지 않다.
- matchReasonsById는 모든 requirement에서 구체적인 비어 있지 않은 판단 이유다.

[작업]
- HIGH이면 taskApplicableById=false이고 taskMissingPointsById와 taskSuggestionsById는 빈 문자열이다.
- HIGH가 아니면 taskApplicableById=true이고 두 문자열은 모두 비어 있지 않다.

[질문]
- generationPolicy.questionGenerationEnabled=true이면 primaryQuestion을 정확히 하나 반환한다.
- false이면 primaryQuestion은 null이다.
- primaryQuestion.relatedRequirementId는 빈 문자열이다. 서버가 일반 질문으로 저장한다.
- evidenceId는 evidenceCatalog에 실제 존재하는 ID 하나만 사용한다.
- question, intent, evaluationFocus, evidenceId는 비어 있지 않다.
- guideProjection의 questionDirection, evaluationFocus, avoidQuestions를 따른다.

[서버 책임]
- readiness는 reason과 limitations만 판단한다.
- status, canGenerateQuestions, source identity, sourceRefs, matchId, questionId, taskId와 저장 형식은 서버가 결정한다.

반환 전에 모든 ById map의 key 집합과 requirementCatalog의 ID 집합이 같은지 확인한다.',
'Do not invent or move evidence IDs across requirements. Do not return server-owned source identity fields.',
TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template WHERE target_json = 'JSON-05' AND version = 'v1.6'
);

UPDATE prompt_template SET is_active = TRUE
WHERE target_json = 'JSON-05' AND version = 'v1.6';

COMMIT;
