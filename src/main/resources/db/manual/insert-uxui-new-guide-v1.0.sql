-- 개발 DB 전용: 디자인 / UX/UI 디자인 / NEW (job_category_id=16) 활성 가이드 최초 등록
-- 적용 전 확인: CATEGORY ACTIVE 중복과 guide_code/version 중복이 없어야 한다.
SELECT guide_id, guide_code, version, status
FROM job_guide_document
WHERE guide_code = 'GUIDE-UXUI-NEW-016' AND version = 'v1.0';

SELECT guide_id, guide_code, version, status
FROM job_guide_document
WHERE scope_type = 'CATEGORY' AND job_category_id = 16 AND status = 'ACTIVE';

START TRANSACTION;

INSERT INTO job_guide_document (
    guide_code, previous_guide_id, scope_type, job_category_id, scope_main_category,
    title, source_type, file_path, status, version, created_by, applicable_scope,
    evaluation_focus, evidence_rules, question_direction, avoid_questions, created_at, updated_at
) VALUES (
    'GUIDE-UXUI-NEW-016', NULL, 'CATEGORY', 16, NULL,
    'UX/UI 디자이너(NEW) 분석 가이드', 'DIRECT_INPUT',
    'src/main/resources/guides/uxui-new-v1.0.md', 'ACTIVE', 'v1.0', 1,
    '디자인 / UX/UI 디자인 / NEW',
    JSON_ARRAY('userProblemDefinition', 'userFlowDesign', 'prototypeIteration', 'usabilityValidation', 'crossFunctionalCollaboration'),
    JSON_ARRAY('입력 marker에 있는 근거만 사용한다.', '팀 산출물과 지원자 본인 역할을 구분한다.', '문서에 없는 사용자 조사·지표 개선 경험을 추정하지 않는다.'),
    JSON_ARRAY('사용자 문제와 설계 가설을 연결한 과정을 질문한다.', '사용성 피드백이 화면 또는 플로우에 반영된 근거를 질문한다.', '개발·기획 협업에서의 제약 조율과 본인 판단을 질문한다.'),
    JSON_ARRAY('포트폴리오에 없는 도구 숙련도나 리딩 경험을 전제하지 않는다.', '팀 성과를 개인의 UX 개선 성과로 단정하지 않는다.'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

SET @uxui_new_guide_id = LAST_INSERT_ID();

INSERT INTO job_guide_chunk (guide_id, chunk_index, title, content, content_summary, embedding_ref, created_at) VALUES
(@uxui_new_guide_id, 0, '사용자 문제와 흐름 설계',
'UX/UI 디자이너는 사용자 맥락과 불편을 관찰한 뒤, 해결할 문제를 명확히 정의하고 우선순위를 정할 수 있어야 한다.
사용자 여정, 정보 구조, 화면 전환 흐름에서 어떤 가설을 세웠고 어떤 근거로 설계 결정을 했는지 확인한다.
와이어프레임과 프로토타입이 단순 화면 제작이 아니라 사용자 과업을 더 쉽게 만드는 과정으로 설명되는지 검증한다.',
'사용자 문제 정의, 정보 구조, 사용자 플로우와 프로토타입 설계 근거', NULL, CURRENT_TIMESTAMP),
(@uxui_new_guide_id, 1, '검증과 협업 경험',
'사용성 테스트, 인터뷰, 정성 피드백 또는 행동 데이터가 있었다면 어떤 관찰을 어떤 UI 개선으로 연결했는지 확인한다.
개발자와 기획자 협업에서는 구현 제약, 정책, 일정 사이에서 어떤 대안을 제안했고 지원자 본인이 맡은 의사결정은 무엇인지 구분한다.
근거 없는 사용자 조사 경험, 디자인 시스템 운영, 정량 성과는 가정하지 않으며 입력 자료에 확인되는 역할과 결과만 평가한다.',
'사용성 검증 결과의 반영, 개발·기획 협업, 개인 역할과 결과의 구분', NULL, CURRENT_TIMESTAMP);

SELECT guide_id, guide_code, version, scope_type, job_category_id, status, created_by
FROM job_guide_document
WHERE guide_id = @uxui_new_guide_id;

SELECT chunk_index, title, content_summary
FROM job_guide_chunk
WHERE guide_id = @uxui_new_guide_id
ORDER BY chunk_index;

COMMIT;
