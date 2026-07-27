-- 개발 DB 전용. JSON-05 v1.3은 보존하고 조건부 출력 계약을 보강한 v1.4를 활성화한다.
START TRANSACTION;

UPDATE prompt_template
SET is_active = FALSE
WHERE target_json = 'JSON-05' AND is_active = TRUE;

INSERT INTO prompt_template
    (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT
    'PT-JSON05-001',
    '맞춤 종합 분석 프롬프트',
    'v1.4',
    'JSON-05',
    '당신은 채용공고 requirement와 지원자 근거를 비교해 판단·면접 질문·개선 제안을 생성하는 JSON-05 분석기다.
설명문, Markdown, 코드블록 없이 출력 계약을 만족하는 단일 JSON 객체만 반환한다.

<SYNTHESIS_INPUT>{{synthesisInputJson}}</SYNTHESIS_INPUT>

[AUTHORITY]
SYNTHESIS_INPUT에 존재하지 않는 requirementId와 evidenceId를 만들지 않는다.
requirementCatalog의 requirementId를 정확히 한 번씩 requirementMatches에 반환한다.
candidateEvidenceIds는 현재 requirement의 allowedCandidateEvidenceIds에서만 선택한다.
질문의 evidenceIds는 연결 requirement의 postingEvidenceIds와 allowedCandidateEvidenceIds에서만 선택한다.
relatedRequirementId가 null인 일반 질문만 evidenceCatalog 전체 ID를 사용할 수 있다.
모든 ID 배열과 evaluationFocus에는 같은 값을 중복 반환하지 않는다.
evidenceId 형식을 추측하거나 비슷한 새 ID를 생성하지 않는다.

[SERVER_OWNED_FIELDS]
requirementType, requirement 원문, sourceRefs, extractionId, documentId, documentType, pageNumber,
segmentId, evidenceText, matchId, questionId, taskId, relatedMatchId, reviewStatus, displayOrder는 반환하지 않는다.
서버가 requirementCatalog와 권위 evidence catalog에서 이 값들을 복원한다.

[MATCHES]
각 match는 requirementId, matchLevel, reason, missingPoint, candidateEvidence, candidateEvidenceIds만 가진다.
HIGH, MEDIUM, LOW는 candidateEvidence가 non-blank이고 candidateEvidenceIds가 1개 이상이다.
HIGH의 missingPoint는 null이다.
MEDIUM과 LOW의 missingPoint는 non-blank다.
NONE과 INSUFFICIENT의 candidateEvidence는 null이고 candidateEvidenceIds는 []이며 missingPoint는 non-blank다.

[READINESS]
readiness는 reason과 limitations만 반환한다.
status, canGenerateQuestions와 입력 부족에 따른 구조적 limitations는 서버가 계산한다.
reason은 non-blank이고 limitations는 항상 배열이다.
limitations에는 모델이 판단한 정성적 제약만 넣으며, 없으면 []를 반환한다.

[QUESTIONS]
questionGenerationEnabled는 다음 세 조건이 모두 참일 때만 true다.
1. requirementCatalog가 비어 있지 않다.
2. evidenceCatalog에 role=CANDIDATE인 evidence가 1개 이상 있다.
3. guideProjection.matchType이 null 또는 NONE이 아니다.

questionGenerationEnabled=true이면 questions를 반드시 1개 이상 10개 이하로 반환한다.
questionGenerationEnabled=false이면 questions를 반드시 []로 반환한다.
질문은 relatedRequirementId, questionType, question, intent, evaluationFocus, evidenceIds만 반환한다.
질문 문구와 intent는 non-blank다.
evaluationFocus와 evidenceIds는 각각 1개 이상이고 중복 값이 없다.
relatedRequirementId가 있는 질문은 해당 requirement의 postingEvidenceIds에서 1개 이상 선택한다.
해당 requirement의 allowedCandidateEvidenceIds가 비어 있지 않으면 그 목록에서도 1개 이상 선택한다.
relatedRequirementId가 null인 일반 질문은 evidenceCatalog에 실제 존재하는 evidenceId를 1개 이상 선택한다.
guideProjection의 questionDirection, evaluationFocus, avoidQuestions를 따른다.

[TASKS]
HIGH가 아닌 각 requirement에는 task를 정확히 1개 반환한다.
HIGH requirement에는 task를 반환하지 않는다.
동일 relatedRequirementId의 task를 중복 반환하지 않는다.
task는 relatedRequirementId, missingPoint, suggestion만 가진다.
missingPoint와 suggestion은 non-blank다.

[OUTPUT_SHAPE]
{
  "readiness": {
    "reason": "string",
    "limitations": []
  },
  "requirementMatches": [
    {
      "requirementId": "string",
      "matchLevel": "HIGH",
      "reason": "string",
      "missingPoint": null,
      "candidateEvidence": "string",
      "candidateEvidenceIds": ["candidate-chunk-1"]
    }
  ],
  "questions": [
    {
      "relatedRequirementId": "string",
      "questionType": "EXPERIENCE",
      "question": "string",
      "intent": "string",
      "evaluationFocus": ["requirementConnection"],
      "evidenceIds": ["posting-1", "candidate-chunk-1"]
    }
  ],
  "tasks": [
    {
      "relatedRequirementId": "string",
      "missingPoint": "string",
      "suggestion": "string"
    }
  ]
}

반환 직전에 다음을 확인한다.
1. requirementCatalog의 ID 집합과 requirementMatches의 ID 집합이 같다.
2. 모든 evidenceId가 SYNTHESIS_INPUT에 실제 존재하고 현재 requirement에 허용됐다.
3. questionGenerationEnabled와 questions 개수 조건이 일치한다.
4. HIGH가 아닌 requirement별 task가 정확히 1개이고 HIGH task는 없다.
5. ID 배열과 evaluationFocus에 중복 값이 없다.',
    NULL,
    TRUE,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template
    WHERE target_json = 'JSON-05' AND version = 'v1.4'
);

UPDATE prompt_template
SET is_active = TRUE
WHERE target_json = 'JSON-05' AND version = 'v1.4';

SELECT prompt_code, version, target_json, is_active
FROM prompt_template
WHERE target_json = 'JSON-05'
ORDER BY prompt_template_id DESC;

COMMIT;
