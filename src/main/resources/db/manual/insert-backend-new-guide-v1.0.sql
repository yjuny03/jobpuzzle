-- 목적: 개발 DB에서 backend-new 가이드의 중복 버전과 CATEGORY ACTIVE 매칭 충돌을 적용 전에 확인한다.
SELECT guide_id, guide_code, version, status
FROM job_guide_document
WHERE guide_code = 'GUIDE-BACKEND-NEW-001' AND version = 'v1.0';

-- 목적: analysisCaseId=2의 job_category_id=1과 동일한 CATEGORY ACTIVE 가이드가 여러 건인지 확인한다.
SELECT guide_id, guide_code, version, status
FROM job_guide_document
WHERE scope_type = 'CATEGORY' AND job_category_id = 1 AND status = 'ACTIVE';

START TRANSACTION;

-- 목적: IT·개발/백엔드 개발/NEW 직무에 정확히 매칭되는 활성 가이드 문서를 최초 등록한다.
INSERT INTO job_guide_document (
    guide_code, previous_guide_id, scope_type, job_category_id, scope_main_category,
    title, source_type, file_path, status, version, created_by, applicable_scope,
    evaluation_focus, evidence_rules, question_direction, avoid_questions, created_at, updated_at
) VALUES (
    'GUIDE-BACKEND-NEW-001', NULL, 'CATEGORY', 1, NULL,
    '백엔드 개발자(NEW) 분석 가이드', 'DIRECT_INPUT',
    'src/main/resources/guides/backend-new-v1.0.md', 'ACTIVE', 'v1.0', 1,
    'IT·개발 / 백엔드 개발 / NEW',
    JSON_ARRAY('apiDesign', 'dataModeling', 'ownRole', 'problemSolving', 'testability'),
    JSON_ARRAY('입력 marker에 있는 근거만 사용한다.', '지원자 본인 역할과 팀 성과를 구분한다.', '기술 선택 이유와 트랜잭션 경계를 확인한다.'),
    JSON_ARRAY('API 설계 의도와 예외 처리 판단을 질문한다.', '데이터 모델과 성능 개선의 실제 기여를 질문한다.', '문제 상황부터 결과까지의 경험 흐름을 확인한다.'),
    JSON_ARRAY('입력 근거 없이 배포·운영 경험을 단정하지 않는다.', '팀 전체 성과를 개인 성과로 가정하지 않는다.'),
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- 목적: AUTO_INCREMENT로 생성된 guide_id에 정본의 두 chunk를 index 순서대로 고정 저장한다.
SET @backend_new_guide_id = LAST_INSERT_ID();

INSERT INTO job_guide_chunk (guide_id, chunk_index, title, content, content_summary, embedding_ref, created_at) VALUES
(@backend_new_guide_id, 0, '백엔드 기본 역량',
'백엔드 개발자는 HTTP 요청과 응답 구조, REST API 설계, 인증·인가 흐름, 예외 처리 방식을 근거와 함께 설명할 수 있어야 한다.
Java와 Spring Boot 기반의 계층 분리, 의존성 관리, 테스트 가능한 코드 구조를 확인한다.
관계형 데이터베이스 모델링, JPA 사용 범위, 조회 성능과 트랜잭션 경계를 실제 경험 근거로 검증한다.',
'REST API, Spring Boot, 데이터 모델링과 트랜잭션 기본 역량', NULL, CURRENT_TIMESTAMP),
(@backend_new_guide_id, 1, '경험 검증 기준',
'프로젝트 경험은 문제 상황, 본인 역할, 해결 행동, 결과를 구분해 확인한다.
지원자가 직접 수행한 API 설계, 데이터 모델링, 성능 개선, 협업 의사결정의 범위를 구체적으로 질문한다.
근거 없는 기술 숙련도 단정, 팀 전체 성과의 개인 성과화, 입력 자료에 없는 배포·운영 경험 추정은 피한다.',
'본인 역할과 문제 해결 경험을 검증하는 질문 기준', NULL, CURRENT_TIMESTAMP);

-- 목적: 등록한 문서 1건과 chunk 2건이 정확히 생성됐는지 확인한 뒤 COMMIT 한다.
SELECT guide_id, guide_code, version, scope_type, job_category_id, status, source_type, created_by
FROM job_guide_document WHERE guide_id = @backend_new_guide_id;
SELECT chunk_index, title, content_summary
FROM job_guide_chunk WHERE guide_id = @backend_new_guide_id ORDER BY chunk_index;
COMMIT;
