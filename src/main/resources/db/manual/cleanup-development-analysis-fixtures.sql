-- 개발 DB 전용 fixture 정리. 운영 DB에서 실행 금지.
-- 첫 SELECT 결과가 삭제 대상이다. 결과를 확인한 뒤에만 아래 DELETE 구문을 실행한다.
-- 범위는 고정 fixture 사용자·그 사용자의 case/snapshot·고정 fixture guide 코드로 한정한다.

DROP TEMPORARY TABLE IF EXISTS tmp_fixture_users;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_cases;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_snapshots;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_documents;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_guides;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_categories;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_question_sets;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_sessions;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_session_questions;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_messages;
DROP TEMPORARY TABLE IF EXISTS tmp_fixture_evaluations;

CREATE TEMPORARY TABLE tmp_fixture_users AS
SELECT user_id FROM `user`
WHERE login_id IN ('fixture-backend-new', 'fixture-uxui-new');
CREATE TEMPORARY TABLE tmp_fixture_cases AS
SELECT ac.analysis_case_id FROM analysis_case ac
JOIN tmp_fixture_users fu ON fu.user_id = ac.user_id;
CREATE TEMPORARY TABLE tmp_fixture_snapshots AS
SELECT ais.snapshot_id FROM analysis_input_snapshot ais
JOIN tmp_fixture_cases fc ON fc.analysis_case_id = ais.analysis_case_id;
CREATE TEMPORARY TABLE tmp_fixture_documents AS
SELECT ud.document_id FROM user_document ud
JOIN tmp_fixture_users fu ON fu.user_id = ud.user_id;
CREATE TEMPORARY TABLE tmp_fixture_categories AS
SELECT DISTINCT jc.job_category_id FROM job_category jc
WHERE (
        (jc.main_category = 'FIXTURE-IT·개발' AND jc.sub_category = 'FIXTURE-백엔드 개발' AND jc.career_level = 'NEW')
     OR (jc.main_category = 'FIXTURE-디자인' AND jc.sub_category = 'FIXTURE-UX/UI 디자인' AND jc.career_level = 'NEW')
      )
  AND (
      jc.job_category_id IN (SELECT default_job_category_id FROM `user` WHERE user_id IN (SELECT user_id FROM tmp_fixture_users))
      OR jc.job_category_id IN (SELECT job_category_id FROM analysis_case WHERE analysis_case_id IN (SELECT analysis_case_id FROM tmp_fixture_cases))
  );
CREATE TEMPORARY TABLE tmp_fixture_guides AS
SELECT jgd.guide_id FROM job_guide_document jgd
JOIN tmp_fixture_users fu ON fu.user_id = jgd.created_by
WHERE (jgd.guide_code = 'FIXTURE-GUIDE-BACKEND-NEW' AND jgd.version = 'v1.0')
   OR (jgd.guide_code = 'FIXTURE-GUIDE-UXUI-NEW' AND jgd.version = 'v1.0');
CREATE TEMPORARY TABLE tmp_fixture_question_sets AS
SELECT qs.question_set_id FROM question_set qs
JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = qs.snapshot_id;
CREATE TEMPORARY TABLE tmp_fixture_sessions AS
SELECT session_id FROM interview_session
WHERE user_id IN (SELECT user_id FROM tmp_fixture_users)
   OR snapshot_id IN (SELECT snapshot_id FROM tmp_fixture_snapshots)
   OR question_set_id IN (SELECT question_set_id FROM tmp_fixture_question_sets);
CREATE TEMPORARY TABLE tmp_fixture_session_questions AS
SELECT session_question_id FROM interview_session_question
WHERE session_id IN (SELECT session_id FROM tmp_fixture_sessions);
CREATE TEMPORARY TABLE tmp_fixture_messages AS
SELECT message_id FROM interview_message
WHERE session_question_id IN (SELECT session_question_id FROM tmp_fixture_session_questions);
CREATE TEMPORARY TABLE tmp_fixture_evaluations AS
SELECT evaluation_id FROM answer_evaluation
WHERE session_question_id IN (SELECT session_question_id FROM tmp_fixture_session_questions)
   OR answer_message_id IN (SELECT message_id FROM tmp_fixture_messages);

-- 삭제 전 대상 SELECT: fixture 식별자/범위가 기대와 다르면 여기서 중단한다.
SELECT 'users' AS target, GROUP_CONCAT(user_id ORDER BY user_id) AS ids, COUNT(*) AS count FROM tmp_fixture_users
UNION ALL SELECT 'cases', GROUP_CONCAT(analysis_case_id ORDER BY analysis_case_id), COUNT(*) FROM tmp_fixture_cases
UNION ALL SELECT 'snapshots', GROUP_CONCAT(snapshot_id ORDER BY snapshot_id), COUNT(*) FROM tmp_fixture_snapshots
UNION ALL SELECT 'documents', GROUP_CONCAT(document_id ORDER BY document_id), COUNT(*) FROM tmp_fixture_documents
UNION ALL SELECT 'guides', GROUP_CONCAT(guide_id ORDER BY guide_id), COUNT(*) FROM tmp_fixture_guides
UNION ALL SELECT 'categories', GROUP_CONCAT(job_category_id ORDER BY job_category_id), COUNT(*) FROM tmp_fixture_categories
UNION ALL SELECT 'question_sets', GROUP_CONCAT(question_set_id ORDER BY question_set_id), COUNT(*) FROM tmp_fixture_question_sets
UNION ALL SELECT 'sessions', GROUP_CONCAT(session_id ORDER BY session_id), COUNT(*) FROM tmp_fixture_sessions;
SELECT u.user_id, u.login_id, ac.analysis_case_id, ais.snapshot_id, jgd.guide_id, jgd.guide_code
FROM tmp_fixture_users fu
JOIN `user` u ON u.user_id = fu.user_id
LEFT JOIN analysis_case ac ON ac.user_id = u.user_id
LEFT JOIN analysis_input_snapshot ais ON ais.analysis_case_id = ac.analysis_case_id
LEFT JOIN job_guide_document jgd ON jgd.created_by = u.user_id
ORDER BY u.user_id, ac.analysis_case_id, ais.snapshot_id, jgd.guide_id;

START TRANSACTION;

-- 면접·평가·리포트 자식 → 부모 (테이블은 FK가 없는 scalar 관계도 명시적으로 정리한다).
DELETE FROM follow_up_question
WHERE evaluation_id IN (SELECT evaluation_id FROM tmp_fixture_evaluations)
   OR question_message_id IN (SELECT message_id FROM tmp_fixture_messages);
DELETE wtl FROM weakness_tag_log wtl
WHERE wtl.user_id IN (SELECT user_id FROM tmp_fixture_users)
   OR wtl.session_id IN (SELECT session_id FROM tmp_fixture_sessions)
   OR wtl.evaluation_id IN (SELECT evaluation_id FROM tmp_fixture_evaluations);
DELETE ae FROM answer_evaluation ae JOIN tmp_fixture_evaluations fe ON fe.evaluation_id = ae.evaluation_id;
DELETE im FROM interview_message im JOIN tmp_fixture_messages fm ON fm.message_id = im.message_id;
DELETE isq FROM interview_session_question isq JOIN tmp_fixture_session_questions fsq ON fsq.session_question_id = isq.session_question_id;
DELETE ims FROM improvement_suggestion ims
JOIN final_report fr ON fr.report_id = ims.report_id
JOIN tmp_fixture_sessions fs ON fs.session_id = fr.session_id;
DELETE fr FROM final_report fr JOIN tmp_fixture_sessions fs ON fs.session_id = fr.session_id;
DELETE ins FROM interview_session ins JOIN tmp_fixture_sessions fs ON fs.session_id = ins.session_id;
DELETE wts FROM weakness_tag_status wts WHERE wts.user_id IN (SELECT user_id FROM tmp_fixture_users);

-- 분석 결과 자식 → snapshot. AI log를 참조하는 모든 FK 자식을 먼저 삭제한다.
DELETE ap FROM action_plan ap JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = ap.snapshot_id;
DELETE iq FROM interview_question iq JOIN tmp_fixture_question_sets fqs ON fqs.question_set_id = iq.question_set_id;
DELETE qs FROM question_set qs JOIN tmp_fixture_question_sets fqs ON fqs.question_set_id = qs.question_set_id;
DELETE gcc FROM guide_context_chunk gcc
JOIN guide_context_result gcr ON gcr.guide_context_result_id = gcc.guide_context_result_id
WHERE gcr.user_id IN (SELECT user_id FROM tmp_fixture_users)
  AND gcr.input_reference_id IN (SELECT CAST(snapshot_id AS CHAR) FROM tmp_fixture_snapshots);
DELETE FROM guide_context_result
WHERE user_id IN (SELECT user_id FROM tmp_fixture_users)
  AND input_reference_id IN (SELECT CAST(snapshot_id AS CHAR) FROM tmp_fixture_snapshots);
DELETE cas FROM confirmed_analysis_snapshot cas
JOIN job_posting_analysis jpa ON jpa.analysis_id = cas.job_posting_analysis_id
JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = jpa.snapshot_id;
DELETE rrc FROM requirement_retrieval_chunk rrc
JOIN requirement_retrieval_result rrr ON rrr.retrieval_result_id = rrc.retrieval_result_id
JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = rrr.snapshot_id;
DELETE rrr FROM requirement_retrieval_result rrr JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = rrr.snapshot_id;
DELETE rr FROM readiness_result rr JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = rr.snapshot_id;
DELETE mar FROM match_analysis_result mar JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = mar.snapshot_id;
DELETE jpa FROM job_posting_analysis jpa JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = jpa.snapshot_id;
DELETE cma FROM candidate_material_analysis cma JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = cma.snapshot_id;

-- fixture snapshot/guide에 연결된 AI 로그의 self-FK를 먼저 끊고 삭제한다.
UPDATE ai_call_log acl
SET acl.parent_ai_call_log_id = NULL, acl.reused_from_call_id = NULL
WHERE acl.guide_id IN (SELECT guide_id FROM tmp_fixture_guides)
   OR acl.input_reference_id IN (SELECT CAST(snapshot_id AS CHAR) FROM tmp_fixture_snapshots);
DELETE acl FROM ai_call_log acl
WHERE acl.guide_id IN (SELECT guide_id FROM tmp_fixture_guides)
   OR acl.input_reference_id IN (SELECT CAST(snapshot_id AS CHAR) FROM tmp_fixture_snapshots);

-- snapshot/source/case 및 문서 자식 → 부모.
DELETE amc FROM analysis_material_chunk amc JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = amc.snapshot_id;
DELETE aiss FROM analysis_input_snapshot_source aiss JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = aiss.snapshot_id;
DELETE ais FROM analysis_input_snapshot ais JOIN tmp_fixture_snapshots fs ON fs.snapshot_id = ais.snapshot_id;
DELETE acs FROM analysis_case_source acs JOIN tmp_fixture_cases fc ON fc.analysis_case_id = acs.analysis_case_id;
DELETE ac FROM analysis_case ac JOIN tmp_fixture_cases fc ON fc.analysis_case_id = ac.analysis_case_id;
DELETE udf FROM user_document_file udf JOIN tmp_fixture_documents fd ON fd.document_id = udf.document_id;
DELETE jp FROM job_posting jp
WHERE jp.user_id IN (SELECT user_id FROM tmp_fixture_users)
   OR jp.job_category_id IN (SELECT job_category_id FROM tmp_fixture_categories)
   OR jp.document_id IN (SELECT document_id FROM tmp_fixture_documents)
   OR jp.company_info_document_id IN (SELECT document_id FROM tmp_fixture_documents);
UPDATE document_extraction de SET de.base_extraction_id = NULL
WHERE de.document_id IN (SELECT document_id FROM tmp_fixture_documents);
DELETE de FROM document_extraction de JOIN tmp_fixture_documents fd ON fd.document_id = de.document_id;
DELETE ud FROM user_document ud JOIN tmp_fixture_documents fd ON fd.document_id = ud.document_id;

-- fixture guide 및 사용자/직무 부모. 외부 데이터는 코드·ID 범위 밖이므로 삭제하지 않는다.
DELETE jgc FROM job_guide_chunk jgc JOIN tmp_fixture_guides fg ON fg.guide_id = jgc.guide_id;
UPDATE job_guide_document jgd SET jgd.previous_guide_id = NULL WHERE jgd.guide_id IN (SELECT guide_id FROM tmp_fixture_guides);
DELETE jgd FROM job_guide_document jgd JOIN tmp_fixture_guides fg ON fg.guide_id = jgd.guide_id;
DELETE rl FROM recommendation_log rl
WHERE rl.user_id IN (SELECT user_id FROM tmp_fixture_users)
   OR rl.criteria_job_category_id IN (SELECT job_category_id FROM tmp_fixture_categories);
DELETE jar FROM job_analysis_report jar WHERE jar.job_category_id IN (SELECT job_category_id FROM tmp_fixture_categories);
DELETE rt FROM refresh_token rt
WHERE rt.username IN ('fixture-backend-new', 'fixture-uxui-new');
DELETE u FROM `user` u JOIN tmp_fixture_users fu ON fu.user_id = u.user_id;
DELETE jc FROM job_category jc JOIN tmp_fixture_categories fc ON fc.job_category_id = jc.job_category_id;

COMMIT;
