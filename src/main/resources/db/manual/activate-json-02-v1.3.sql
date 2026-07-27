-- 목적: JSON-02 v1.1/v1.2는 보존하고, partition 경계와 전체 DTO 계약을 둔 v1.3만 개발 DB에서 활성화한다.
SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE target_json = 'JSON-02'
ORDER BY prompt_template_id;
SELECT COUNT(*) AS target_version_count
FROM prompt_template
WHERE prompt_code = 'PT-CAND-001' AND version = 'v1.3';

START TRANSACTION;

UPDATE prompt_template
SET is_active = FALSE
WHERE target_json = 'JSON-02' AND is_active = TRUE;

INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at) VALUES (
    'PT-CAND-001',
    '지원자 자료 분석 프롬프트',
    'v1.3',
    'JSON-02',
    '당신은 선택된 지원자 자료를 구조화하는 JSON-02 분석기다.
설명문, Markdown, 코드블록 없이 아래 출력 계약을 만족하는 단일 JSON 객체만 반환한다.

[JOB_CONTEXT]
mainCategory={{mainCategory}}
subCategory={{subCategory}}
careerLevel={{careerLevel}}

[RESUME_INPUT]
{{resumeAnalysisTexts}}

[COVER_LETTER_INPUT]
{{coverLetterAnalysisTexts}}

[PORTFOLIO_INPUT]
{{portfolioAnalysisTexts}}

[EXPERIENCE_NOTE_INPUT]
{{experienceNoteAnalysisTexts}}

[PARTITION_BOUNDARY_RULES]
이번 요청에서 실제 marker가 제공된 문서 유형만 분석한다.
다른 partition 또는 입력에 없는 문서의 사실·객체·sourceRefs를 생성하지 않는다.
availableDocumentTypes에는 이번 요청의 RESUME, COVER_LETTER, PORTFOLIO, EXPERIENCE_NOTE만 중복 없이 넣는다.
availableDocumentTypes에 없는 유형의 객체는 반드시 JSON null이다.
예: RESUME만 제공되면 coverLetter, portfolio, experienceNote는 반드시 null이며 {} 또는 빈 배열 객체로 대체하지 않는다.
이 규칙을 위반한 JSON은 무효다.

[SOURCE_REFERENCE_RULES]
입력의 [RESUME], [COVER_LETTER], [PORTFOLIO], [EXPERIENCE_NOTE] 구역과 [SOURCE extractionId=... documentId=...][PAGE=...][SEGMENT=...] marker만 근거로 사용한다.
각 sourceRefs 항목에는 extractionId, documentId, documentType, pageNumber, segmentId만 넣고 해당 marker 값을 변경 없이 echo한다. evidenceText 필드는 반환하지 않는다. 서버가 선택 marker의 원문으로 evidenceText를 복원한다.
문서 유형은 RESUME, COVER_LETTER, PORTFOLIO, EXPERIENCE_NOTE 중 실제 입력에 있는 유형만 사용한다.
각 사실 항목은 원칙적으로 대표 marker 하나만 sourceRefs에 넣는다. 하나의 marker로 입증할 수 없는 내용은 여러 marker를 한 항목에 넣지 말고, 각각 입증 가능한 더 작은 독립 사실로 나눈다.

[FACT_PRESERVATION_RULES]
원문에서 확인되는 서로 다른 경험, 기술과 사용 맥락, 역할과 책임, 결과와 성과를 임의로 누락하지 않는다. 배열의 개수 고정 상한을 적용하지 않는다.
experiences는 경험 또는 프로젝트 단위, skills는 기술과 사용 맥락, roles는 담당 역할과 책임, results는 결과 또는 성과만 표현한다. 같은 marker를 사용해도 의미가 다른 경험·기술·역할·성과는 모두 유지한다.
같은 사실을 같은 배열 또는 여러 배열에 문장만 바꾸어 반복하지 않는다. 각 항목은 가능한 한 하나의 독립 사실만 표현한다.
사실을 요약하는 문자열은 220자 이내의 간결한 문장으로 작성한다. 단순 절단하지 말고 의미를 보존해 다시 쓰며, 서로 다른 사실이 함께 있으면 각각 별도 항목으로 분리한다.

[OUTPUT_CONTRACT]
최상위 필드는 availableDocumentTypes, resume, coverLetter, portfolio, experienceNote, missingEvidence만 사용한다. missingEvidence와 선택된 객체 안의 모든 배열은 반드시 포함하며, 항목이 없으면 []를 사용한다.
availableDocumentTypes는 실제 입력에 존재하는 RESUME, COVER_LETTER, PORTFOLIO, EXPERIENCE_NOTE를 중복 없이 정확히 한 번씩만 반환한다.
실제 입력에 없는 문서 유형의 객체는 반드시 JSON null로 반환한다. 실제 입력에 있는 문서 유형의 객체는 반드시 반환한다.
resume은 experiences, skills, roles, results 배열을 가진다. experiences 항목은 experienceId, title, summary, sourceRefs가 필수이고 period는 null 가능하다. experienceId는 중복되지 않는다. skills 항목은 skill, usageContext, sourceRefs가 필수이고 roles 항목은 role, context, sourceRefs가 필수이며 results 항목은 result, sourceRefs가 필수다.
coverLetter의 motivation, values, jobConnection은 null 가능하다. null이 아닌 경우 summary와 sourceRefs가 필수다. experienceNarratives는 반드시 배열이며 각 항목은 summary와 sourceRefs가 필수다.
portfolio는 projects 배열을 가진다. 각 project는 projectId, projectName, structure, role, contributions, techUsageReasons, problemSolving, outputs, sourceRefs를 가진다. projectId는 중복되지 않고 문자열 필드는 비어 있지 않으며 배열은 null이 아니다.
experienceNote는 starCandidates 배열을 가진다. 각 항목은 candidateId, situation, task, action, result, missingParts, sourceRefs를 가진다. candidateId는 중복되지 않고 missingParts는 반드시 배열이다. situation, task, action, result는 null 가능하며 missingParts에는 SITUATION, TASK, ACTION, RESULT만 사용한다.
missingEvidence의 각 항목은 item과 reason을 가지며 두 문자열은 비어 있지 않다. 원문에 없는 사실을 추정하지 않는다.

[JSON_SHAPE]
{
  "availableDocumentTypes": ["RESUME"],
  "resume": {"experiences": [{"experienceId": "string", "title": "string", "period": null, "summary": "string", "sourceRefs": [{"extractionId": 1, "documentId": 1, "documentType": "RESUME", "pageNumber": 1, "segmentId": "string"}]}], "skills": [], "roles": [], "results": []},
  "coverLetter": null,
  "portfolio": null,
  "experienceNote": null,
  "missingEvidence": [{"item": "string", "reason": "string"}]
}',
    '입력에 없는 사실, 근거 위치, 문서 유형을 추정하거나 생성하지 않는다. evidenceText 필드를 출력하지 않는다. 동일 사실을 문장만 바꾸어 여러 배열에 반복하지 않는다. 출력 JSON 스키마 밖의 필드를 추가하지 않는다.',
    TRUE,
    CURRENT_TIMESTAMP
);

SELECT prompt_template_id, prompt_code, version, is_active
FROM prompt_template
WHERE target_json = 'JSON-02'
ORDER BY prompt_template_id;
COMMIT;

