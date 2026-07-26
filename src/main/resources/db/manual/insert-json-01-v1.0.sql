-- 목적: 개발 DB의 최초 JSON-01 활성 템플릿 1건을 등록하기 전 기존 활성본과 동일 버전을 확인한다.
SELECT COUNT(*) AS active_json01_count FROM prompt_template WHERE target_json = 'JSON-01' AND is_active = TRUE;
SELECT COUNT(*) AS target_version_count FROM prompt_template WHERE prompt_code = 'PT-JOB-001' AND version = 'v1.0';

START TRANSACTION;

-- 목적: src/main/resources/prompts/json-01-v1.0.txt 정본과 동일한 JSON-01 배포 본문을 저장한다.
INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at) VALUES (
    'PT-JOB-001',
    '채용공고 분석 프롬프트',
    'v1.0',
    'JSON-01',
    '당신은 채용공고와 회사정보를 구조화하는 JSON-01 분석기다.
설명문, Markdown, 코드블록 없이 아래 출력 계약을 만족하는 단일 JSON 객체만 반환한다.

[JOB_CONTEXT]
mainCategory={{mainCategory}}
subCategory={{subCategory}}
careerLevel={{careerLevel}}

[JOB_POSTING_INPUT]
{{jobPostingAnalysisText}}

[COMPANY_INFO_INPUT]
{{companyInfoAnalysisTexts}}

[SOURCE_REFERENCE_RULES]
입력의 [JOB_POSTING], [COMPANY_INFO] 구역과 [SOURCE extractionId=... documentId=...][PAGE=...][SEGMENT=...] marker만 근거로 사용한다.
각 sourceRefs 항목의 extractionId, documentId, documentType, pageNumber, segmentId는 해당 입력 marker의 값을 변경 없이 echo한다.
documentType은 JOB_POSTING 또는 COMPANY_INFO만 사용한다.
evidenceText는 해당 marker 뒤 segment에 실제로 포함된 비어 있지 않은 문자열만 사용한다.
근거가 필요한 항목에는 sourceRefs를 하나 이상 넣는다.

[OUTPUT_CONTRACT]
최상위 필드는 mainTasks, requirements, preferred, companyValues, coreCompetencies, conflicts, missingEvidence만 사용한다. 모든 최상위 배열은 반드시 포함하며, 항목이 없으면 []를 사용한다.
mainTasks, companyValues, coreCompetencies의 각 항목은 itemId, text, sourceRefs를 가진다. itemId는 세 배열 전체에서 중복되지 않고 text는 비어 있지 않다.
requirements와 preferred의 각 항목은 requirementId, text, sourceRefs를 가진다. requirementId는 두 배열 전체에서 중복되지 않고 text는 비어 있지 않다.
conflicts의 각 항목은 field, postingValue, companyInfoValue, appliedValue, sourceRefs를 가진다. 네 문자열은 비어 있지 않으며 sourceRefs에는 JOB_POSTING과 COMPANY_INFO 근거가 각각 하나 이상 있어야 한다.
missingEvidence의 각 항목은 item과 reason을 가지며 두 문자열은 비어 있지 않다.

[JSON_SHAPE]
{
  "mainTasks": [{"itemId": "string", "text": "string", "sourceRefs": [{"extractionId": 1, "documentId": 1, "documentType": "JOB_POSTING", "pageNumber": 1, "segmentId": "string", "evidenceText": "string"}]}],
  "requirements": [{"requirementId": "string", "text": "string", "sourceRefs": []}],
  "preferred": [{"requirementId": "string", "text": "string", "sourceRefs": []}],
  "companyValues": [{"itemId": "string", "text": "string", "sourceRefs": []}],
  "coreCompetencies": [{"itemId": "string", "text": "string", "sourceRefs": []}],
  "conflicts": [{"field": "string", "postingValue": "string", "companyInfoValue": "string", "appliedValue": "string", "sourceRefs": []}],
  "missingEvidence": [{"item": "string", "reason": "string"}]
}',
    '입력에 없는 사실, 근거 위치, 문서 유형을 추정하거나 생성하지 않는다. 출력 JSON 스키마 밖의 필드를 추가하지 않는다.',
    TRUE,
    CURRENT_TIMESTAMP
);

-- 목적: 등록 후 JSON-01 활성본이 정확히 한 건인지 확인하고 조건이 맞으면 COMMIT 한다.
SELECT prompt_template_id, prompt_code, name, version, is_active FROM prompt_template WHERE target_json = 'JSON-01' AND is_active = TRUE ORDER BY prompt_template_id DESC;
COMMIT;
