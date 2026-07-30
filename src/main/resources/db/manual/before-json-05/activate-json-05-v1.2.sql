-- 개발 DB 전용: JSON-05 retrieval 원문 중복 제거 입력 계약(v1.2)을 활성화한다. 실행 전 v1.1 정본 존재를 확인한다.
SELECT COUNT(*) AS source_version_count FROM prompt_template WHERE prompt_code = 'PT-JSON05-001' AND version = 'v1.1';
SELECT COUNT(*) AS target_version_count FROM prompt_template WHERE prompt_code = 'PT-JSON05-001' AND version = 'v1.2';
START TRANSACTION;
UPDATE prompt_template SET is_active = FALSE WHERE target_json = 'JSON-05' AND is_active = TRUE;
INSERT INTO prompt_template (prompt_code, name, version, target_json, template_text, forbidden_rules, is_active, created_at)
SELECT prompt_code, name, 'v1.2', target_json,
REPLACE(template_text,
'evidencedRequirementMatches의 candidate 판단과 candidateSourceRefs는 RETRIEVED_EVIDENCE에서 현재 requirementId에 연결된 chunks만 사용한다.
다른 requirementId의 chunk, 검색되지 않은 candidate 근거, JSON-02 전체에서 임의 선택한 candidate 근거를 사용하지 않는다.
retrievalStatus가 EMPTY이면 해당 requirement의 candidate 근거가 부족한 것으로 판단한다.
retrieval sourceRef는 선택된 단일 chunk의 extractionId, documentId, documentType, pageStart를 각각 extractionId, documentId, documentType, pageNumber로 사용한다.
retrieval sourceRef의 segmentId는 반드시 null이고 evidenceText는 선택된 단일 chunk content 안의 실제 문자열이며 같은 sourceRef를 중복 반환하지 않는다.',
'RETRIEVED_EVIDENCE는 requirements와 candidateEvidence로 정규화되어 있다. requirements[].candidateEvidenceIds는 해당 requirementId에 허용된 candidateEvidence[].evidenceId 목록이다.
evidencedRequirementMatches의 candidate 판단과 candidateSourceRefs는 현재 requirementId의 candidateEvidenceIds가 가리키는 candidateEvidence만 사용한다.
다른 requirementId의 evidenceId, 목록에 없는 candidateEvidence, 검색되지 않은 candidate 근거, JSON-02 전체에서 임의 선택한 candidate 근거를 사용하지 않는다.
retrievalStatus가 EMPTY이거나 candidateEvidenceIds가 비어 있으면 해당 requirement의 candidate 근거가 부족한 것으로 판단한다.
선택한 candidateEvidence의 extractionId, documentId, documentType, pageStart를 retrieval sourceRef의 extractionId, documentId, documentType, pageNumber로 각각 사용한다.
retrieval sourceRef의 segmentId는 반드시 null이고 evidenceText는 선택한 candidateEvidence.content 안의 실제 문자열이며 같은 sourceRef를 중복 반환하지 않는다.'),
forbidden_rules, TRUE, NOW()
FROM prompt_template WHERE prompt_code = 'PT-JSON05-001' AND version = 'v1.1';
COMMIT;
