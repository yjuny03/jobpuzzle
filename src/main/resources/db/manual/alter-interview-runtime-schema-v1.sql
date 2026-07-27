-- BASIC·WEAKNESS_REVIEW QuestionSet은 분석 스냅샷 없이 생성되므로 snapshot_id가 NULL 가능해야 한다.
ALTER TABLE question_set
    MODIFY COLUMN snapshot_id BIGINT NULL;

-- 현재 최종 엔티티의 WeaknessTagStatus PK 컬럼은 status_id를 사용한다.
-- 기존 DB에 이미 status_id가 있다면 아래 확인 결과만 보고 추가 변경하지 않는다.
SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'weakness_tag_status'
  AND COLUMN_NAME IN ('status_id', 'weakness_status_id');
