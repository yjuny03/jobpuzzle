-- 개발 DB 전용: 기존 JSON-04 context chunk의 title/content를 불변 snapshot으로 고정한다.
START TRANSACTION;
ALTER TABLE guide_context_chunk
    ADD COLUMN IF NOT EXISTS chunk_title_snapshot VARCHAR(200) NULL,
    ADD COLUMN IF NOT EXISTS chunk_content_snapshot LONGTEXT NULL,
    ADD COLUMN IF NOT EXISTS chunk_content_hash_snapshot VARCHAR(64) NULL;

UPDATE guide_context_chunk gcc
JOIN job_guide_chunk jgc ON jgc.chunk_id = gcc.chunk_id
SET gcc.chunk_title_snapshot = jgc.title,
    gcc.chunk_content_snapshot = jgc.content,
    gcc.chunk_content_hash_snapshot = SHA2(jgc.content, 256)
WHERE gcc.chunk_content_snapshot IS NULL;
COMMIT;
