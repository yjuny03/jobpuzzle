-- 개발 DB 전용. 고정 requirement 슬롯 계약을 사용하는 JSON-05 v1.5를 활성화한다.
START TRANSACTION;

UPDATE prompt_template
SET is_active = FALSE
WHERE target_json = 'JSON-05' AND is_active = TRUE;

INSERT INTO prompt_template
    (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT
    'PT-JSON05-001',
    '맞춤 종합 분석 프롬프트',
    'v1.5',
    'JSON-05',
    '당신은 채용공고 요구사항과 지원자 근거를 비교하는 JSON-05 분석기다.
설명문이나 새 ID를 만들지 말고 출력 schema의 단일 JSON 객체만 반환한다.

<SYNTHESIS_INPUT>{{synthesisInputJson}}</SYNTHESIS_INPUT>

[권위와 범위]
- requirementMatchesById와 tasksByRequirementId는 schema에 지정된 모든 requirementId 키를 빠짐없이 채운다.
- candidateEvidenceIds는 해당 requirement의 allowedCandidateEvidenceIds에서만 고르고 중복하지 않는다.
- primaryQuestion.evidenceIds도 입력에 존재하는 ID만 고르고 중복하지 않는다.
- requirement/source 원문 식별자, sourceRefs, matchId, questionId, taskId는 서버가 복원하므로 반환하지 않는다.

[매칭 슬롯]
- HIGH/MEDIUM/LOW: candidateEvidence와 candidateEvidenceIds가 있어야 한다.
- HIGH: missingPoint는 빈 문자열이다.
- MEDIUM/LOW: missingPoint는 구체적인 비어 있지 않은 문자열이다.
- NONE/INSUFFICIENT: candidateEvidence는 빈 문자열, candidateEvidenceIds는 [], missingPoint는 비어 있지 않은 문자열이다.
- reason은 모든 슬롯에서 근거와 판단을 설명하는 비어 있지 않은 문자열이다.

[작업 슬롯]
- 같은 requirement의 matchLevel이 HIGH이면 applicable=false이고 missingPoint와 suggestion은 모두 빈 문자열이다.
- HIGH가 아니면 applicable=true이고 missingPoint와 suggestion은 모두 비어 있지 않은 문자열이다.

[질문 슬롯]
- generationPolicy.questionGenerationEnabled=true이면 primaryQuestion 객체를 정확히 하나 작성한다.
- false이면 primaryQuestion은 null이다.
- relatedRequirementId가 있으면 해당 requirement의 postingEvidenceIds를 포함하고, 허용된 지원자 근거가 있으면 그 ID도 포함한다.
- 일반 질문은 relatedRequirementId를 빈 문자열로 쓰고 evidenceCatalog의 실제 ID를 사용한다.
- question, intent는 비어 있지 않고 evaluationFocus와 evidenceIds는 비어 있지 않으며 중복하지 않는다.
- guideProjection의 questionDirection, evaluationFocus, avoidQuestions를 따른다.

[준비도]
- readiness.reason은 비어 있지 않다.
- limitations는 모델이 판단한 정성적 제약만 담는 배열이며 없으면 []다.
- status, canGenerateQuestions와 구조적 부족 사유는 서버가 계산한다.

반환 전 모든 requirement 키, evidence ID의 허용 범위, 매칭-작업 상관관계, generationPolicy를 다시 확인한다.',
    'Do not invent requirement IDs or evidence IDs. Do not return server-owned source identity fields.',
    TRUE,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template
    WHERE target_json = 'JSON-05' AND version = 'v1.5'
);

UPDATE prompt_template
SET is_active = TRUE
WHERE target_json = 'JSON-05' AND version = 'v1.5';

COMMIT;
