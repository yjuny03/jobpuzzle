-- 목적: 개발 테스트 case(user_id=1, snapshot_id=1)에만 연결된 과거 NONE context 후보를 먼저 확인한다.
SELECT guide_context_result_id, user_id, input_reference_id, match_type, guide_id
FROM guide_context_result
WHERE user_id = 1 AND input_reference_id = '1' AND match_type = 'NONE';

START TRANSACTION;

-- 목적: 대상 NONE context의 자식 chunk를 먼저 삭제해 FK 제약을 보존한다.
DELETE context_chunk
FROM guide_context_chunk context_chunk
JOIN guide_context_result context_result
  ON context_result.guide_context_result_id = context_chunk.guide_context_result_id
WHERE context_result.user_id = 1 AND context_result.input_reference_id = '1'
  AND context_result.match_type = 'NONE';

-- 목적: 확인된 개발 테스트 snapshot의 NONE context만 삭제하고 정상 context는 유지한다.
DELETE FROM guide_context_result
WHERE user_id = 1 AND input_reference_id = '1' AND match_type = 'NONE';

-- 목적: 삭제 후 동일 테스트 대상의 NONE context가 남지 않았는지 확인한다.
SELECT guide_context_result_id
FROM guide_context_result
WHERE user_id = 1 AND input_reference_id = '1' AND match_type = 'NONE';
COMMIT;
