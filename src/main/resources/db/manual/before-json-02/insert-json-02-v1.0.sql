-- 목적: 개발 DB의 최초 JSON-02 활성 템플릿 1건을 등록하기 전 기존 활성본과 동일 버전을 확인한다.
SELECT COUNT(*) AS active_json02_count FROM prompt_template WHERE target_json = 'JSON-02' AND is_active = TRUE;
SELECT COUNT(*) AS target_version_count FROM prompt_template WHERE prompt_code = 'PT-CAND-001' AND version = 'v1.0';

START TRANSACTION;

-- 목적: src/main/resources/prompts/before-json-02/json-02-v1.0.txt 보관 정본과 동일한 JSON-02 배포 본문을 저장한다.
INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at) VALUES (
    'PT-CAND-001',
    '지원자 자료 분석 프롬프트',
    'v1.0',
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

[SOURCE_REFERENCE_RULES]
입력의 [RESUME], [COVER_LETTER], [PORTFOLIO], [EXPERIENCE_NOTE] 구역과 [SOURCE extractionId=... documentId=...][PAGE=...][SEGMENT=...] marker만 근거로 사용한다.
각 sourceRefs 항목의 extractionId, documentId, documentType, pageNumber, segmentId는 해당 입력 marker의 값을 변경 없이 echo한다.
documentType은 RESUME, COVER_LETTER, PORTFOLIO, EXPERIENCE_NOTE 중 실제 입력에 있는 유형만 사용한다.
evidenceText는 해당 marker 뒤 segment에 실제로 포함된 비어 있지 않은 문자열만 사용한다.
근거가 필요한 항목에는 sourceRefs를 하나 이상 넣는다.

[OUTPUT_CONTRACT]
최상위 필드는 availableDocumentTypes, resume, coverLetter, portfolio, experienceNote, missingEvidence만 사용한다. missingEvidence와 선택된 객체 안의 모든 배열은 반드시 포함하며, 항목이 없으면 []를 사용한다.
availableDocumentTypes는 실제 입력에 존재하는 RESUME, COVER_LETTER, PORTFOLIO, EXPERIENCE_NOTE를 중복 없이 정확히 한 번씩만 반환한다.
실제 입력에 없는 문서 유형의 객체는 반드시 JSON null로 반환한다. 실제 입력에 있는 문서 유형의 객체는 반드시 반환한다.
resume은 experiences, skills, roles, results 배열을 가진다. experiences 항목은 experienceId, title, summary, sourceRefs가 필수이고 period는 null 가능하다. experienceId는 중복되지 않는다. skills 항목은 skill, usageContext, sourceRefs가 필수이고 roles 항목은 role, context, sourceRefs가 필수이며 results 항목은 result, sourceRefs가 필수다.
coverLetter의 motivation, values, jobConnection은 null 가능하다. null이 아닌 경우 summary와 sourceRefs가 필수다. experienceNarratives는 반드시 배열이며 각 항목은 summary와 sourceRefs가 필수다.
portfolio는 projects 배열을 가진다. 각 project는 projectId, projectName, structure, role, contributions, techUsageReasons, problemSolving, outputs, sourceRefs를 가진다. projectId는 중복되지 않고 문자열 필드는 비어 있지 않으며 배열은 null이 아니다.
experienceNote는 starCandidates 배열을 가진다. 각 항목은 candidateId, situation, task, action, result, missingParts, sourceRefs를 가진다. candidateId는 중복되지 않고 missingParts는 반드시 배열이다. situation, task, action, result는 null 가능하며 missingParts에는 SITUATION, TASK, ACTION, RESULT만 사용한다.
missingEvidence의 각 항목은 item과 reason을 가지며 두 문자열은 비어 있지 않다.

[JSON_SHAPE]
{
  "availableDocumentTypes": ["RESUME"],
  "resume": {"experiences": [{"experienceId": "string", "title": "string", "period": null, "summary": "string", "sourceRefs": [{"extractionId": 1, "documentId": 1, "documentType": "RESUME", "pageNumber": 1, "segmentId": "string", "evidenceText": "string"}]}], "skills": [], "roles": [], "results": []},
  "coverLetter": null,
  "portfolio": null,
  "experienceNote": null,
  "missingEvidence": [{"item": "string", "reason": "string"}]
}',
    '선택되지 않은 문서 유형의 결과를 만들지 않는다. 입력에 없는 사실, 근거 위치, 문서 유형을 추정하거나 생성하지 않는다.',
    TRUE,
    CURRENT_TIMESTAMP
);

-- 목적: 등록 후 JSON-02 활성본이 정확히 한 건인지 확인하고 조건이 맞으면 COMMIT 한다.
SELECT prompt_template_id, prompt_code, name, version, is_active FROM prompt_template WHERE target_json = 'JSON-02' AND is_active = TRUE ORDER BY prompt_template_id DESC;
COMMIT;
