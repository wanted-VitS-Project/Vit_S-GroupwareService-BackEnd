ALTER TABLE activity_log
    ADD COLUMN resource_name VARCHAR(255) NULL COMMENT 'Block 내부 데이터 표시명 스냅샷'
        AFTER resource_id;
