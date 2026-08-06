ALTER TABLE file_index
    ADD COLUMN index_attempt_id VARCHAR(36) NULL COMMENT '현재 파일 인덱싱 시도 ID' AFTER file_version_id;
