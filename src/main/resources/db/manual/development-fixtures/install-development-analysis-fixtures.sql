-- 개발 DB 전용 독립 분석 fixture 설치. 운영 DB에서 실행 금지.
-- 실행 전 반드시 verify-development-analysis-fixtures.sql의 dry-run 결과가 모두 0인지 확인한다.
-- fixture 식별자가 존재하면 프로시저가 SIGNAL로 즉시 중단하며, 기존 데이터를 갱신하지 않는다.

DELIMITER //

CREATE PROCEDURE install_development_analysis_fixtures()
BEGIN
    DECLARE fixture_collision_count INT DEFAULT 0;

    SELECT COUNT(*) INTO fixture_collision_count
    FROM (
        SELECT user_id AS id FROM `user`
        WHERE login_id IN ('fixture-backend-new', 'fixture-uxui-new')
           OR email IN ('fixture-backend-new@example.test', 'fixture-uxui-new@example.test')
        UNION ALL
        SELECT job_category_id FROM job_category
        WHERE (main_category = 'FIXTURE-IT·개발' AND sub_category = 'FIXTURE-백엔드 개발' AND career_level = 'NEW')
           OR (main_category = 'FIXTURE-디자인' AND sub_category = 'FIXTURE-UX/UI 디자인' AND career_level = 'NEW')
        UNION ALL
        SELECT guide_id FROM job_guide_document
        WHERE (guide_code = 'FIXTURE-GUIDE-BACKEND-NEW' AND version = 'v1.0')
           OR (guide_code = 'FIXTURE-GUIDE-UXUI-NEW' AND version = 'v1.0')
    ) fixture_collision;

    IF fixture_collision_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'fixture collision detected: run cleanup only after inspecting its target SELECT output';
    END IF;

    START TRANSACTION;

    INSERT INTO job_category (main_category, sub_category, career_level, created_at) VALUES
    ('FIXTURE-IT·개발', 'FIXTURE-백엔드 개발', 'NEW', CURRENT_TIMESTAMP),
    ('FIXTURE-디자인', 'FIXTURE-UX/UI 디자인', 'NEW', CURRENT_TIMESTAMP);

    SELECT job_category_id INTO @fixture_backend_category_id
    FROM job_category
    WHERE main_category = 'FIXTURE-IT·개발' AND sub_category = 'FIXTURE-백엔드 개발' AND career_level = 'NEW';
    SELECT job_category_id INTO @fixture_uxui_category_id
    FROM job_category
    WHERE main_category = 'FIXTURE-디자인' AND sub_category = 'FIXTURE-UX/UI 디자인' AND career_level = 'NEW';

    INSERT INTO `user` (login_id, password, email, name, role, default_job_category_id, login_fail_count, is_locked, status, created_at, updated_at) VALUES
    ('fixture-backend-new', '$2a$10$qDwAr90MV83Y5.Uenk/6kO5z76ulslKNCF8IvaK/y/VaTAZFh3LCq', 'fixture-backend-new@example.test', 'Fixture Backend', 'USER', @fixture_backend_category_id, 0, FALSE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('fixture-uxui-new', '$2a$10$qDwAr90MV83Y5.Uenk/6kO5z76ulslKNCF8IvaK/y/VaTAZFh3LCq', 'fixture-uxui-new@example.test', 'Fixture UXUI', 'USER', @fixture_uxui_category_id, 0, FALSE, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    SELECT user_id INTO @fixture_backend_user_id FROM `user` WHERE login_id = 'fixture-backend-new';
    SELECT user_id INTO @fixture_uxui_user_id FROM `user` WHERE login_id = 'fixture-uxui-new';

    INSERT INTO user_document (user_id, document_type, source_type, display_name, keep_original, created_at, updated_at) VALUES
    (@fixture_backend_user_id, 'JOB_POSTING', 'TEXT', 'FIXTURE Backend Job Posting', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@fixture_backend_user_id, 'RESUME', 'TEXT', 'FIXTURE Backend Resume', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@fixture_uxui_user_id, 'JOB_POSTING', 'TEXT', 'FIXTURE UXUI Job Posting', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@fixture_uxui_user_id, 'RESUME', 'TEXT', 'FIXTURE UXUI Resume', FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    INSERT INTO document_extraction (document_id, major_version, minor_version, extraction_status, version_status, content, page_count, ocr_applied, confirmed_at, created_at)
    SELECT ud.document_id, 1, 0, 'SUCCESS', 'CONFIRMED',
           CASE ud.document_type
               WHEN 'JOB_POSTING' THEN '[1페이지]\nJava Spring Boot REST API, JPA, 테스트 경험이 필요합니다.'
               ELSE '[1페이지]\nSpring Boot API와 JPA 기반 주문 기능을 구현하고 단위 테스트를 작성했습니다.'
           END,
           1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM user_document ud WHERE ud.user_id = @fixture_backend_user_id;
    INSERT INTO document_extraction (document_id, major_version, minor_version, extraction_status, version_status, content, page_count, ocr_applied, confirmed_at, created_at)
    SELECT ud.document_id, 1, 0, 'SUCCESS', 'CONFIRMED',
           CASE ud.document_type
               WHEN 'JOB_POSTING' THEN '[1페이지]\n사용자 흐름 설계, Figma 프로토타입, 사용성 검증 경험이 필요합니다.'
               ELSE '[1페이지]\n사용자 인터뷰를 바탕으로 가입 흐름을 재설계하고 Figma 프로토타입으로 개발자와 협업했습니다.'
           END,
           1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM user_document ud WHERE ud.user_id = @fixture_uxui_user_id;

    INSERT INTO analysis_case (user_id, job_category_id, status, created_at, updated_at) VALUES
    (@fixture_backend_user_id, @fixture_backend_category_id, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@fixture_uxui_user_id, @fixture_uxui_category_id, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    SELECT analysis_case_id INTO @fixture_backend_case_id
    FROM analysis_case WHERE user_id = @fixture_backend_user_id AND job_category_id = @fixture_backend_category_id AND status = 'DRAFT';
    SELECT analysis_case_id INTO @fixture_uxui_case_id
    FROM analysis_case WHERE user_id = @fixture_uxui_user_id AND job_category_id = @fixture_uxui_category_id AND status = 'DRAFT';

    INSERT INTO analysis_case_source (analysis_case_id, extraction_id, document_type, created_at)
    SELECT @fixture_backend_case_id, de.extraction_id, ud.document_type, CURRENT_TIMESTAMP
    FROM document_extraction de JOIN user_document ud ON ud.document_id = de.document_id
    WHERE ud.user_id = @fixture_backend_user_id;
    INSERT INTO analysis_case_source (analysis_case_id, extraction_id, document_type, created_at)
    SELECT @fixture_uxui_case_id, de.extraction_id, ud.document_type, CURRENT_TIMESTAMP
    FROM document_extraction de JOIN user_document ud ON ud.document_id = de.document_id
    WHERE ud.user_id = @fixture_uxui_user_id;

    INSERT INTO job_guide_document (guide_code, scope_type, job_category_id, title, source_type, file_path, status, version, created_by, applicable_scope, evaluation_focus, evidence_rules, question_direction, avoid_questions, created_at, updated_at) VALUES
    ('FIXTURE-GUIDE-BACKEND-NEW', 'CATEGORY', @fixture_backend_category_id, 'Fixture 백엔드 NEW 가이드', 'DIRECT_INPUT', 'src/main/resources/guides/fixture-backend-new-v1.0.md', 'ACTIVE', 'v1.0', @fixture_backend_user_id, 'fixture backend', JSON_ARRAY('api','data'), JSON_ARRAY('marker only'), JSON_ARRAY('role'), JSON_ARRAY('no assumption'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FIXTURE-GUIDE-UXUI-NEW', 'CATEGORY', @fixture_uxui_category_id, 'Fixture UXUI NEW 가이드', 'DIRECT_INPUT', 'src/main/resources/guides/fixture-uxui-new-v1.0.md', 'ACTIVE', 'v1.0', @fixture_uxui_user_id, 'fixture uxui', JSON_ARRAY('flow','validation'), JSON_ARRAY('marker only'), JSON_ARRAY('decision'), JSON_ARRAY('no assumption'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    SELECT guide_id INTO @fixture_backend_guide_id FROM job_guide_document WHERE guide_code = 'FIXTURE-GUIDE-BACKEND-NEW' AND version = 'v1.0';
    SELECT guide_id INTO @fixture_uxui_guide_id FROM job_guide_document WHERE guide_code = 'FIXTURE-GUIDE-UXUI-NEW' AND version = 'v1.0';

    INSERT INTO job_guide_chunk (guide_id, chunk_index, title, content, content_summary, created_at) VALUES
    (@fixture_backend_guide_id, 0, 'API와 데이터', 'API 설계와 데이터 모델의 본인 역할을 확인한다.', '백엔드 설계', CURRENT_TIMESTAMP),
    (@fixture_backend_guide_id, 1, '테스트', '예외 처리와 테스트 근거를 확인한다.', '백엔드 검증', CURRENT_TIMESTAMP),
    (@fixture_uxui_guide_id, 0, '사용자 흐름', '사용자 문제와 플로우 설계 근거를 확인한다.', 'UX 흐름', CURRENT_TIMESTAMP),
    (@fixture_uxui_guide_id, 1, '검증과 협업', '사용성 검증과 개발 협업 근거를 확인한다.', 'UX 검증', CURRENT_TIMESTAMP);

    COMMIT;
END //

DELIMITER ;

CALL install_development_analysis_fixtures();
DROP PROCEDURE install_development_analysis_fixtures;
