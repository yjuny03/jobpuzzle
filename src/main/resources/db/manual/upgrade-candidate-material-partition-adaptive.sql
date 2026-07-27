-- 개발 DB 전용: JSON-02 adaptive split/retry 저장 모델.
ALTER TABLE candidate_material_partition
    ADD COLUMN parent_partition_id BIGINT NULL,
    ADD COLUMN partition_depth INT NOT NULL DEFAULT 0,
    ADD COLUMN failure_kind VARCHAR(40) NULL,
    ADD CONSTRAINT fk_candidate_material_partition_parent
        FOREIGN KEY (parent_partition_id) REFERENCES candidate_material_partition(partition_id);
CREATE INDEX idx_candidate_material_partition_parent_status
    ON candidate_material_partition(parent_partition_id, status);
CREATE INDEX idx_candidate_material_partition_reuse
    ON candidate_material_partition(input_fingerprint, status);
