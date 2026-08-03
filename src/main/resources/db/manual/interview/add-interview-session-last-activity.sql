-- 진행 중인 면접 목록의 최신 활동 정렬 기준
ALTER TABLE interview_session
    ADD COLUMN last_activity_at DATETIME NULL;

-- 기존 세션은 확정·시작·생성 시각 순서로 초기값을 보정한다.
UPDATE interview_session
SET last_activity_at = COALESCE(completed_at, started_at, created_at)
WHERE last_activity_at IS NULL;
