-- 개발 DB fixture dry-run 및 설치 후 검증. 읽기 전용 SQL이며 데이터를 변경하지 않는다.
-- 모든 collision_count가 0이면 install SQL 실행 후보이며, 1 이상이면 설치를 중단한다.

SELECT 'collision_users' AS check_name, COUNT(*) AS collision_count
FROM `user`
WHERE login_id IN ('fixture-backend-new', 'fixture-uxui-new')
   OR email IN ('fixture-backend-new@example.test', 'fixture-uxui-new@example.test')
UNION ALL
SELECT 'collision_categories', COUNT(*)
FROM job_category
WHERE (main_category = 'FIXTURE-IT·개발' AND sub_category = 'FIXTURE-백엔드 개발' AND career_level = 'NEW')
   OR (main_category = 'FIXTURE-디자인' AND sub_category = 'FIXTURE-UX/UI 디자인' AND career_level = 'NEW')
UNION ALL
SELECT 'collision_guides', COUNT(*)
FROM job_guide_document
WHERE (guide_code = 'FIXTURE-GUIDE-BACKEND-NEW' AND version = 'v1.0')
   OR (guide_code = 'FIXTURE-GUIDE-UXUI-NEW' AND version = 'v1.0');

-- cleanup이 삭제할 최상위 fixture 범위. 설치 전에는 모두 빈 결과여야 한다.
SELECT u.user_id, u.login_id, ac.analysis_case_id, ac.status, ais.snapshot_id
FROM `user` u
LEFT JOIN analysis_case ac ON ac.user_id = u.user_id
LEFT JOIN analysis_input_snapshot ais ON ais.analysis_case_id = ac.analysis_case_id
WHERE u.login_id IN ('fixture-backend-new', 'fixture-uxui-new')
ORDER BY u.user_id, ac.analysis_case_id, ais.snapshot_id;

SELECT jgd.guide_id, jgd.guide_code, jgd.version, jgd.status, COUNT(jgc.chunk_id) AS chunk_count
FROM job_guide_document jgd
LEFT JOIN job_guide_chunk jgc ON jgc.guide_id = jgd.guide_id
WHERE jgd.guide_code IN ('FIXTURE-GUIDE-BACKEND-NEW', 'FIXTURE-GUIDE-UXUI-NEW')
GROUP BY jgd.guide_id, jgd.guide_code, jgd.version, jgd.status
ORDER BY jgd.guide_id;

-- 설치 후 계약 검증: 사용자별 DRAFT case 1건, source 2건, guide별 chunk 2건이어야 한다.
SELECT ac.analysis_case_id, u.login_id, jc.main_category, jc.sub_category, jc.career_level,
       ac.status, COUNT(acs.analysis_case_source_id) AS source_count
FROM analysis_case ac
JOIN `user` u ON u.user_id = ac.user_id
JOIN job_category jc ON jc.job_category_id = ac.job_category_id
LEFT JOIN analysis_case_source acs ON acs.analysis_case_id = ac.analysis_case_id
WHERE u.login_id IN ('fixture-backend-new', 'fixture-uxui-new')
GROUP BY ac.analysis_case_id, u.login_id, jc.main_category, jc.sub_category, jc.career_level, ac.status
ORDER BY ac.analysis_case_id;
