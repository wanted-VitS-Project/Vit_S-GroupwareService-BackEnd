ALTER TABLE activity_log
    DROP FOREIGN KEY fk_al_project;
ALTER TABLE activity_log
    DROP FOREIGN KEY fk_al_block;

ALTER TABLE activity_log
    DROP INDEX idx_al_project,
    DROP INDEX idx_al_resource;

ALTER TABLE activity_log
    DROP COLUMN project_id,
    DROP COLUMN resource_type,
    DROP COLUMN target_name,
    DROP COLUMN privileged_override;

ALTER TABLE activity_log
    MODIFY act ENUM('create','delete','modify') NOT NULL COMMENT '활동 유형',
    MODIFY block_id BIGINT NOT NULL COMMENT '활동이 발생한 블록 ID';

ALTER TABLE activity_log
    ADD CONSTRAINT fk_al_block
        FOREIGN KEY (block_id) REFERENCES block (block_id);
