-- 상위 약점 관점과 AI 자유 형식 세부 진단을 분리한다.
-- 신규 평가는 weakness_tag_log.tag에 서버가 확정한 canonical 관점만 저장하고,
-- 이 컬럼에는 관점별 세부 진단 키워드를 JSON 객체로 저장한다.

ALTER TABLE answer_evaluation
    ADD COLUMN weakness_diagnostics JSON NULL AFTER weakness_tags;

